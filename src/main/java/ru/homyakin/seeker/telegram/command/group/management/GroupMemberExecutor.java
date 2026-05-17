package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.group.action.personage.GetGroupMemberDetails;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupMemberExecutor extends CommandExecutor<GroupMember> {
    private final GroupTgService groupTgService;
    private final GetGroupMemberDetails getGroupMemberDetails;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public GroupMemberExecutor(
        GroupTgService groupTgService,
        GetGroupMemberDetails getGroupMemberDetails,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.getGroupMemberDetails = getGroupMemberDetails;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupMember command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        final var language = groupTg.language();
        final var text = getGroupMemberDetails
            .execute(groupTg.domainGroupId(), command.personageId())
            .map(found -> GroupManagementLocalization.groupMemberProfileCard(
                language,
                found,
                personageService.getEquippedCharacteristics(found.personage().id())
            ))
            .orElseGet(() -> GroupManagementLocalization.groupMemberNotFound(language));
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupTgId())
                .text(text)
                .build()
        );
    }
}
