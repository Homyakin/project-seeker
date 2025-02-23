package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class LeaveGroupMemberCancelExecutor extends CommandExecutor<LeaveGroupMemberCancel> {
    private final GroupUserService groupUserService;
    private final TelegramSender telegramSender;

    public LeaveGroupMemberCancelExecutor(
        GroupUserService groupUserService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(LeaveGroupMemberCancel command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        if (!user.personageId().equals(command.personageId())) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    CommonLocalization.forbiddenAction(groupTg.language())
                )
            );
            return;
        }
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(command.groupTgId())
                .messageId(command.messageId())
                .text(GroupManagementLocalization.leaveGroupCancel(groupTg.language()))
                .build()
        );
    }
}
