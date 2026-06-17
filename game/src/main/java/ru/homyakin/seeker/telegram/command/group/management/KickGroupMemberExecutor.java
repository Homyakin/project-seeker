package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.personage.KickGroupMemberCommand;
import ru.homyakin.seeker.game.group.error.KickGroupMemberError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class KickGroupMemberExecutor extends CommandExecutor<KickGroupMember> {
    private final GroupUserService groupUserService;
    private final KickGroupMemberCommand kickGroupMemberCommand;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public KickGroupMemberExecutor(
        GroupUserService groupUserService,
        KickGroupMemberCommand kickGroupMemberCommand,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.kickGroupMemberCommand = kickGroupMemberCommand;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(KickGroupMember command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        final var prepare = kickGroupMemberCommand.prepare(
            groupTg.domainGroupId(),
            user.personageId(),
            command.targetPersonageId()
        );
        if (prepare.isLeft()) {
            final var text = switch (prepare.getLeft()) {
                case KickGroupMemberError.NotAnAdmin _ ->
                    CommonLocalization.onlyAdminAction(groupTg.language());
                case KickGroupMemberError.PersonageNotInGroup _ ->
                    CommonLocalization.onlyGroupMemberAction(groupTg.language());
                case KickGroupMemberError.CannotKickSelf _ ->
                    GroupManagementLocalization.kickCannotKickSelf(groupTg.language());
                case KickGroupMemberError.TargetNotInGroup _ ->
                    GroupManagementLocalization.kickNotMember(groupTg.language());
                case KickGroupMemberError.KickLeaveInvariantViolated _ ->
                    CommonLocalization.internalError(groupTg.language());
            };
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(command.groupTgId())
                    .text(text)
                    .build()
            );
            return;
        }

        final var target = personageService.getByIdForce(command.targetPersonageId());
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(command.groupTgId())
                .text(GroupManagementLocalization.kickConfirmationRequired(groupTg.language(), target))
                .keyboard(InlineKeyboards.kickGroupMemberConfirmationKeyboard(
                    groupTg.language(),
                    command.targetPersonageId()
                ))
                .build()
        );
    }
}

