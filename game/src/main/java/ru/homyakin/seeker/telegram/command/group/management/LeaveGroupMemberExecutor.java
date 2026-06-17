package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.personage.LeaveGroupMemberCommand;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class LeaveGroupMemberExecutor extends CommandExecutor<LeaveGroupMember> {
    private final GroupUserService groupUserService;
    private final LeaveGroupMemberCommand leaveGroupMemberCommand;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;

    public LeaveGroupMemberExecutor(
        GroupUserService groupUserService,
        LeaveGroupMemberCommand leaveGroupMemberCommand,
        TelegramSender telegramSender,
        PersonageService personageService
    ) {
        this.groupUserService = groupUserService;
        this.leaveGroupMemberCommand = leaveGroupMemberCommand;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
    }

    @Override
    public void execute(LeaveGroupMember command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        final var builder = SendMessageBuilder.builder()
            .chatId(command.groupTgId());
        leaveGroupMemberCommand.execute(user.personageId(), groupTg.domainGroupId())
            .fold(
                error -> switch (error) {
                    case LeaveGroupMemberError.NotGroupMember _ ->
                        builder.text(GroupManagementLocalization.leaveGroupNotMember(groupTg.language()));
                    case LeaveGroupMemberError.LastMember _ ->
                        builder
                            .text(GroupManagementLocalization.leaveGroupLastMemberConfirmation(groupTg.language()))
                            .keyboard(InlineKeyboards.leaveGroupConfirmationKeyboard(groupTg.language(), user.personageId()));
                },
                joinTimeout -> builder.text(GroupManagementLocalization.leaveGroupSuccess(
                    groupTg.language(),
                    personageService.getByIdForce(user.personageId()),
                    joinTimeout
                ))
            );
        telegramSender.send(builder.build());
    }
}
