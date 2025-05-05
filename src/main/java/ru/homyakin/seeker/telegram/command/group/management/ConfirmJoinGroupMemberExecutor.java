package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.personage.JoinGroupMemberCommand;
import ru.homyakin.seeker.game.group.error.CheckGroupMemberAdminError;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ConfirmJoinGroupMemberExecutor extends CommandExecutor<ConfirmJoinGroupMember> {
    private final GroupUserService groupUserService;
    private final JoinGroupMemberCommand joinGroupMemberCommand;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;

    public ConfirmJoinGroupMemberExecutor(
        GroupUserService groupUserService,
        JoinGroupMemberCommand joinGroupMemberCommand,
        TelegramSender telegramSender,
        PersonageService personageService
    ) {
        this.groupUserService = groupUserService;
        this.joinGroupMemberCommand = joinGroupMemberCommand;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
    }

    @Override
    public void execute(ConfirmJoinGroupMember command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        final var builder = EditMessageTextBuilder.builder()
            .messageId(command.messageId())
            .chatId(command.groupTgId());
        final var result = joinGroupMemberCommand.confirm(
            groupTg.domainGroupId(),
            user.personageId(),
            command.personageId()
        );
        final var text = result.fold(
            error -> switch (error) {
                case JoinGroupMemberError.PersonageAlreadyInGroup _ ->
                    GroupManagementLocalization.joinPersonageAlreadyInGroup(groupTg.language());
                case JoinGroupMemberError.PersonageInAnotherGroup _ ->
                    GroupManagementLocalization.joinPersonageInAnotherGroup(groupTg.language());
                case JoinGroupMemberError.GroupNotRegistered _ ->
                    GroupManagementLocalization.groupNotRegisteredAtJoin(groupTg.language());
                case JoinGroupMemberError.PersonageJoinTimeout personageJoinTimeout ->
                    GroupManagementLocalization.joinPersonageTimeout(groupTg.language(), personageJoinTimeout);
                case JoinGroupMemberError.ConfirmationRequired _ -> {
                    builder.keyboard(
                        InlineKeyboards.joinGroupConfirmationKeyboard(groupTg.language(), user.personageId())
                    );
                    yield GroupManagementLocalization.joinPersonageConfirmationRequired(
                        groupTg.language(),
                        personageService.getByIdForce(user.personageId())
                    );
                }
                case CheckGroupMemberAdminError.NotAnAdmin _ -> CommonLocalization.onlyAdminAction(groupTg.language());
                case CheckGroupMemberAdminError.PersonageNotInGroup _ ->
                    GroupManagementLocalization.joinConfirmNotMember(groupTg.language());
            },
            _ -> GroupManagementLocalization.joinPersonageConfirmed(
                groupTg.language(),
                personageService.getByIdForce(command.personageId()),
                personageService.getByIdForce(user.personageId())
            )
        );
        if (result.isLeft() && result.getLeft() instanceof CheckGroupMemberAdminError) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text));
        } else {
            telegramSender.send(builder.text(text).build());
        }
    }
}
