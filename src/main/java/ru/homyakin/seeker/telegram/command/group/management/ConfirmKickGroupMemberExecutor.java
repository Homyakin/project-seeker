package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.personage.KickGroupMemberCommand;
import ru.homyakin.seeker.game.group.error.KickGroupMemberError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.personage.TgPersonageNotificationService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ConfirmKickGroupMemberExecutor extends CommandExecutor<ConfirmKickGroupMember> {
    private final GroupUserService groupUserService;
    private final KickGroupMemberCommand kickGroupMemberCommand;
    private final PersonageService personageService;
    private final GetGroup getGroup;
    private final TgPersonageNotificationService tgPersonageNotificationService;
    private final TelegramSender telegramSender;

    public ConfirmKickGroupMemberExecutor(
        GroupUserService groupUserService,
        KickGroupMemberCommand kickGroupMemberCommand,
        PersonageService personageService,
        GetGroup getGroup,
        TgPersonageNotificationService tgPersonageNotificationService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.kickGroupMemberCommand = kickGroupMemberCommand;
        this.personageService = personageService;
        this.getGroup = getGroup;
        this.tgPersonageNotificationService = tgPersonageNotificationService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ConfirmKickGroupMember command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        final var kickResult = kickGroupMemberCommand.kick(
            groupTg.domainGroupId(),
            user.personageId(),
            command.targetPersonageId()
        );
        if (kickResult.isLeft()) {
            switch (kickResult.getLeft()) {
                case KickGroupMemberError.TargetNotInGroup _ -> telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .chatId(command.groupTgId())
                        .messageId(command.messageId())
                        .text(GroupManagementLocalization.kickNotMember(groupTg.language()))
                        .build()
                );
                case KickGroupMemberError.KickLeaveInvariantViolated _ -> telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .chatId(command.groupTgId())
                        .messageId(command.messageId())
                        .text(CommonLocalization.internalError(groupTg.language()))
                        .build()
                );
                case KickGroupMemberError.NotAnAdmin _ -> telegramSender.send(
                    TelegramMethods.createAnswerCallbackQuery(
                        command.callbackId(),
                        CommonLocalization.onlyAdminAction(groupTg.language())
                    )
                );
                case KickGroupMemberError.PersonageNotInGroup _ -> telegramSender.send(
                    TelegramMethods.createAnswerCallbackQuery(
                        command.callbackId(),
                        CommonLocalization.onlyGroupMemberAction(groupTg.language())
                    )
                );
                case KickGroupMemberError.CannotKickSelf _ -> telegramSender.send(
                    TelegramMethods.createAnswerCallbackQuery(
                        command.callbackId(),
                        GroupManagementLocalization.kickCannotKickSelf(groupTg.language())
                    )
                );
            }
            return;
        }

        final var joinTimeout = kickResult.get();
        final var target = personageService.getByIdForce(command.targetPersonageId());
        final var admin = personageService.getByIdForce(user.personageId());
        final var group = getGroup.forceGet(groupTg.domainGroupId());
        tgPersonageNotificationService.sendNotification(
            command.targetPersonageId(),
            new Notification.KickedFromGroup(group, admin, joinTimeout)
        );
        final var text = GroupManagementLocalization.kickConfirmed(groupTg.language(), target, admin, joinTimeout);

        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(command.groupTgId())
                .messageId(command.messageId())
                .text(text)
                .build()
        );
    }
}

