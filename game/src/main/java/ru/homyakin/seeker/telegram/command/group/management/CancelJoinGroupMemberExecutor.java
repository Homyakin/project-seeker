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
public class CancelJoinGroupMemberExecutor extends CommandExecutor<CancelJoinGroupMember> {
    private final GroupUserService groupUserService;
    private final CheckGroupMemberAdminCommand checkGroupMemberAdminCommand;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public CancelJoinGroupMemberExecutor(
        GroupUserService groupUserService,
        TelegramSender telegramSender,
        PersonageService personageService,
        CheckGroupMemberAdminCommand checkGroupMemberAdminCommand
    ) {
        this.groupUserService = groupUserService;
        this.checkGroupMemberAdminCommand = checkGroupMemberAdminCommand;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
    }

    @Override
    public void execute(CancelJoinGroupMember command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        final var result = checkGroupMemberAdminCommand.execute(groupTg.domainGroupId(), user.personageId());
        if (result.isLeft()) {
            final var text = switch (result.getLeft()) {
                case CheckGroupMemberAdminError.NotAnAdmin _ ->
                    CommonLocalization.onlyAdminAction(groupTg.language());
                case CheckGroupMemberAdminError.PersonageNotInGroup _ ->
                    GroupManagementLocalization.joinConfirmNotMember(groupTg.language());
            };
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text));
        } else {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(groupTg.id())
                    .messageId(command.messageId())
                    .text(
                        GroupManagementLocalization.joinPersonageCanceled(
                            groupTg.language(),
                            personageService.getByIdForce(command.personageId())
                        )
                    )
                    .build()
            );
        }
    }
}
