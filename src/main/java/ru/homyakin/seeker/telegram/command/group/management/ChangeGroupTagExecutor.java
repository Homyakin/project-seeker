package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GroupTagService;
import ru.homyakin.seeker.game.group.error.ChangeTagError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ChangeGroupTagExecutor extends CommandExecutor<ChangeGroupTag> {
    private final GroupUserService groupUserService;
    private final GroupTagService groupTagService;
    private final TelegramSender telegramSender;

    public ChangeGroupTagExecutor(
        GroupUserService groupUserService,
        GroupTagService groupTagService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.groupTagService = groupTagService;

        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ChangeGroupTag command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();
        if (command.tag().isEmpty()) {
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(command.groupTgId())
                    .text(GroupManagementLocalization.incorrectTag(groupTg.language()))
                    .build()
            );
            return;
        }
        final var text = groupTagService.changeTag(groupTg.domainGroupId(), user.personageId(), command.tag().get())
            .fold(
                error -> switch (error) {
                    case ChangeTagError.GroupNotRegistered _ ->
                        CommonLocalization.onlyForRegisteredGroup(groupTg.language());
                    case ChangeTagError.InvalidTag _ ->
                        GroupManagementLocalization.incorrectTag(groupTg.language());
                    case ChangeTagError.NotAdmin _ ->
                        CommonLocalization.onlyAdminAction(groupTg.language());
                    case ChangeTagError.NotEnoughMoney notEnoughMoney ->
                        GroupManagementLocalization.notEnoughMoneyForChangeTag(groupTg.language(), notEnoughMoney.required());
                    case ChangeTagError.PersonageNotInGroup _ ->
                        CommonLocalization.onlyGroupMemberAction(groupTg.language());
                    case ChangeTagError.TagAlreadyTaken _ ->
                        GroupManagementLocalization.tagAlreadyTaken(groupTg.language());
                },
                _ -> GroupManagementLocalization.successChangeTag(groupTg.language(), command.tag().get())
            );

        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupTgId())
                .text(text)
                .build()
        );
    }
}
