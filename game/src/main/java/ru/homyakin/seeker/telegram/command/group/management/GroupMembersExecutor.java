package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.group.action.personage.ListGroupMembersPage;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupMembersExecutor extends CommandExecutor<GroupMembers> {
    private final GroupTgService groupTgService;
    private final ListGroupMembersPage listGroupMembersPage;
    private final TelegramSender telegramSender;

    public GroupMembersExecutor(
        GroupTgService groupTgService,
        ListGroupMembersPage listGroupMembersPage,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.listGroupMembersPage = listGroupMembersPage;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupMembers command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        final var language = groupTg.language();
        final var result = listGroupMembersPage.execute(groupTg.domainGroupId(), command.page());
        final var text = GroupManagementLocalization.groupMembersList(
            language,
            result
        );
        final var keyboard = InlineKeyboards.groupMembersPaginationKeyboard(
            language,
            result.currentPage(),
            result.totalPages()
        );
        if (command.messageId().isEmpty()) {
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(command.groupTgId())
                    .text(text)
                    .keyboard(keyboard)
                    .build()
            );
        } else {
            telegramSender.send(
                EditMessageTextBuilder
                    .builder()
                    .chatId(command.groupTgId())
                    .messageId(command.messageId().orElseThrow())
                    .text(text)
                    .keyboard(keyboard)
                    .build()
            );
        }
    }
}
