package ru.homyakin.seeker.telegram.command.user.group;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.personage.ConfirmLeaveGroupMemberCommand;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class LeaveGroupInPrivateConfirmExecutor extends CommandExecutor<LeaveGroupInPrivateConfirm> {
    private final UserService userService;
    private final ConfirmLeaveGroupMemberCommand confirmLeaveGroupMemberCommand;
    private final PersonageService personageService;
    private final GetGroup getGroup;
    private final TelegramSender telegramSender;

    public LeaveGroupInPrivateConfirmExecutor(
        UserService userService,
        ConfirmLeaveGroupMemberCommand confirmLeaveGroupMemberCommand,
        PersonageService personageService,
        GetGroup getGroup,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.confirmLeaveGroupMemberCommand = confirmLeaveGroupMemberCommand;
        this.personageService = personageService;
        this.getGroup = getGroup;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(LeaveGroupInPrivateConfirm command) {
        final var user = userService.forceGetFromPrivate(command.userId());

        if (!user.personageId().equals(command.personageId())) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    CommonLocalization.forbiddenAction(user.language())
                )
            );
            return;
        }

        final var text = confirmLeaveGroupMemberCommand.execute(user.personageId())
            .fold(
                _ -> GroupManagementLocalization.leaveGroupNotAnyMember(user.language()),
                result -> switch (result.leaveType()) {
                    case NOT_LAST_MEMBER ->
                        GroupManagementLocalization.leaveGroupSuccess(
                            user.language(),
                            personageService.getByIdForce(user.personageId()),
                            result.joinTimeout()
                        );
                    case LAST_MEMBER -> GroupManagementLocalization.leaveGroupLastMemberSuccess(
                        user.language(),
                        personageService.getByIdForce(user.personageId()),
                        getGroup.forceGet(result.groupId()),
                        result.joinTimeout()
                    );
                }
            );
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(text)
                .build()
        );
    }
}

