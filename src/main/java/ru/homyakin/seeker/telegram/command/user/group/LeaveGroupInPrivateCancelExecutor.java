package ru.homyakin.seeker.telegram.command.user.group;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class LeaveGroupInPrivateCancelExecutor extends CommandExecutor<LeaveGroupInPrivateCancel> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public LeaveGroupInPrivateCancelExecutor(
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(LeaveGroupInPrivateCancel command) {
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
        
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(GroupManagementLocalization.leaveGroupCancel(user.language()))
                .build()
        );
    }
}

