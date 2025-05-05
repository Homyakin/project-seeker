package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.personage.JoinGroupMemberCommand;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class JoinGroupMemberExecutor extends CommandExecutor<JoinGroupMember> {
    private final GroupUserService groupUserService;
    private final JoinGroupMemberCommand joinGroupMemberCommand;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;

    public JoinGroupMemberExecutor(
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
    public void execute(JoinGroupMember command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        final var builder = SendMessageBuilder.builder().chatId(command.groupTgId());
        final var text = joinGroupMemberCommand.join(groupTg.domainGroupId(), user.personageId())
            .fold(
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
                },
                group -> GroupManagementLocalization.successJoinGroup(
                    groupTg.language(),
                    personageService.getByIdForce(user.personageId()),
                    group
                )
            );
        telegramSender.send(builder.text(text).build());
    }
}
