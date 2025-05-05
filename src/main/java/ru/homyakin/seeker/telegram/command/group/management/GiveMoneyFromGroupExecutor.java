package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GiveMoneyFromGroupCommand;
import ru.homyakin.seeker.game.group.error.CheckGroupMemberAdminError;
import ru.homyakin.seeker.game.group.error.GiveMoneyFromGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.models.UserType;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GiveMoneyFromGroupExecutor extends CommandExecutor<GiveMoneyFromGroup> {
    private final GroupUserService groupUserService;
    private final GiveMoneyFromGroupCommand giveMoneyFromGroupCommand;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final UserService userService;

    public GiveMoneyFromGroupExecutor(
        GroupUserService groupUserService,
        GiveMoneyFromGroupCommand giveMoneyFromGroupCommand,
        TelegramSender telegramSender,
        PersonageService personageService,
        UserService userService
    ) {
        this.groupUserService = groupUserService;
        this.giveMoneyFromGroupCommand = giveMoneyFromGroupCommand;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.userService = userService;
    }

    @Override
    public void execute(GiveMoneyFromGroup command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        if (command.amount().isEmpty()) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(command.groupTgId())
                    .text(GroupManagementLocalization.incorrectAmount(groupTg.language()))
                    .build()
            );
            return;
        }
        if (command.mention().isEmpty() || command.mention().get().userType() != UserType.USER) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(command.groupTgId())
                    .text(GroupManagementLocalization.incorrectAcceptor(groupTg.language()))
                    .build()
            );
            return;
        }
        final var acceptorUser = userService.getByMention(command.mention().get(), groupTg.id());
        if (acceptorUser.isEmpty()) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(command.groupTgId())
                    .text(GroupManagementLocalization.acceptorNotFound(groupTg.language()))
                    .build()
            );
            return;
        }

        final var donateMoney = Money.from(command.amount().get());
        final var text = giveMoneyFromGroupCommand.execute(
            groupTg.domainGroupId(),
                user.personageId(),
                acceptorUser.get().personageId(),
                donateMoney
            )
            .fold(
                error -> switch (error) {
                    case GiveMoneyFromGroupError.NotEnoughMoney _ ->
                        GroupManagementLocalization.notEnoughMoneyForGive(groupTg.language());
                    case GiveMoneyFromGroupError.PersonageNotMember _ ->
                        GroupManagementLocalization.giveMoneyPersonageNotMember(groupTg.language());
                    case GiveMoneyFromGroupError.GroupNotRegistered _ ->
                        CommonLocalization.onlyForRegisteredGroup(groupTg.language());
                    case GiveMoneyFromGroupError.InvalidAmount _ ->
                        GroupManagementLocalization.incorrectAmount(groupTg.language());
                    case CheckGroupMemberAdminError.NotAnAdmin _ ->
                        CommonLocalization.onlyAdminAction(groupTg.language());
                    case CheckGroupMemberAdminError.PersonageNotInGroup _ ->
                        GroupManagementLocalization.giverNotMember(groupTg.language());
                },
                personage ->
                    GroupManagementLocalization.successGiveMoney(
                        groupTg.language(),
                        personageService.getByIdForce(user.personageId()),
                        personage,
                        donateMoney
                    )
            );
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(command.groupTgId())
                .text(text)
                .build()
        );
    }
}
