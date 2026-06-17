package ru.homyakin.seeker.telegram.command.user.group;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.personage.LeaveGroupMemberCommand;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class LeaveGroupInPrivateExecutor extends CommandExecutor<LeaveGroupInPrivate> {
    private final UserService userService;
    private final LeaveGroupMemberCommand leaveGroupMemberCommand;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public LeaveGroupInPrivateExecutor(
        UserService userService,
        LeaveGroupMemberCommand leaveGroupMemberCommand,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.leaveGroupMemberCommand = leaveGroupMemberCommand;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(LeaveGroupInPrivate command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var personage = personageService.getByIdForce(user.personageId());
        
        final var builder = SendMessageBuilder.builder()
            .chatId(user.id());
        
        leaveGroupMemberCommand.execute(personage.id())
            .fold(
                error -> switch (error) {
                    case LeaveGroupMemberError.NotGroupMember _ ->
                        builder.text(GroupManagementLocalization.leaveGroupNotAnyMember(user.language()));
                    case LeaveGroupMemberError.LastMember _ ->
                        builder
                            .text(GroupManagementLocalization.leaveGroupLastMemberConfirmation(user.language()))
                            .keyboard(InlineKeyboards.leaveGroupConfirmationKeyboard(user.language(), personage.id()));
                },
                joinTimeout -> builder.text(GroupManagementLocalization.leaveGroupSuccess(
                    user.language(),
                    personage,
                    joinTimeout
                ))
            );
        telegramSender.send(builder.build());
    }
}

