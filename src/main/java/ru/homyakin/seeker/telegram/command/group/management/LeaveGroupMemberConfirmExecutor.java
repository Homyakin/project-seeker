package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.personage.ConfirmLeaveGroupMemberCommand;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class LeaveGroupMemberConfirmExecutor extends CommandExecutor<LeaveGroupMemberConfirm> {
    private final GroupUserService groupUserService;
    private final ConfirmLeaveGroupMemberCommand confirmLeaveGroupMemberCommand;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final GetGroup getGroup;

    public LeaveGroupMemberConfirmExecutor(
        GroupUserService groupUserService,
        ConfirmLeaveGroupMemberCommand confirmLeaveGroupMemberCommand,
        TelegramSender telegramSender,
        PersonageService personageService,
        GetGroup getGroup
    ) {
        this.groupUserService = groupUserService;
        this.confirmLeaveGroupMemberCommand = confirmLeaveGroupMemberCommand;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.getGroup = getGroup;
    }

    @Override
    public void execute(LeaveGroupMemberConfirm command) {
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

        final var text = confirmLeaveGroupMemberCommand.execute(user.personageId(), groupTg.domainGroupId())
            .fold(
                _ -> GroupManagementLocalization.leaveGroupNotMember(groupTg.language()),
                result -> switch (result) {
                    case NOT_LAST_MEMBER ->
                        GroupManagementLocalization.leaveGroupSuccess(
                            groupTg.language(),
                            personageService.getByIdForce(user.personageId())
                        );
                    case LAST_MEMBER -> GroupManagementLocalization.leaveGroupLastMemberSuccess(
                        groupTg.language(),
                        personageService.getByIdForce(user.personageId()),
                        getGroup.forceGet(groupTg.domainGroupId())
                    );
                }
            );
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(command.groupTgId())
                .messageId(command.messageId())
                .text(text)
                .build()
        );
    }
}
