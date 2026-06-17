package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupMemberAdminCommand;
import ru.homyakin.seeker.game.group.error.CheckGroupMemberAdminError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class CancelKickGroupMemberExecutor extends CommandExecutor<CancelKickGroupMember> {
    private final GroupUserService groupUserService;
    private final CheckGroupMemberAdminCommand checkGroupMemberAdminCommand;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public CancelKickGroupMemberExecutor(
        GroupUserService groupUserService,
        CheckGroupMemberAdminCommand checkGroupMemberAdminCommand,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.checkGroupMemberAdminCommand = checkGroupMemberAdminCommand;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(CancelKickGroupMember command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        final var adminCheck = checkGroupMemberAdminCommand.execute(groupTg.domainGroupId(), user.personageId());
        if (adminCheck.isLeft()) {
            final var text = switch (adminCheck.getLeft()) {
                case CheckGroupMemberAdminError.NotAnAdmin _ ->
                    CommonLocalization.onlyAdminAction(groupTg.language());
                case CheckGroupMemberAdminError.PersonageNotInGroup _ ->
                    CommonLocalization.onlyGroupMemberAction(groupTg.language());
            };
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text));
            return;
        }

        final var target = personageService.getByIdForce(command.targetPersonageId());
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(groupTg.id())
                .messageId(command.messageId())
                .text(GroupManagementLocalization.kickCanceled(groupTg.language(), target))
                .build()
        );
    }
}

