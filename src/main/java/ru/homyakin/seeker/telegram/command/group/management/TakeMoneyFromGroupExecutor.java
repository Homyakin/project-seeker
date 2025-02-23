package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.TakeMoneyFromGroupCommand;
import ru.homyakin.seeker.game.group.error.TakeMoneyFromGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TakeMoneyFromGroupExecutor extends CommandExecutor<TakeMoneyFromGroup> {
    private final GroupUserService groupUserService;
    private final TakeMoneyFromGroupCommand takeMoneyFromGroupCommand;
    private final TelegramSender telegramSender;

    public TakeMoneyFromGroupExecutor(
        GroupUserService groupUserService,
        TakeMoneyFromGroupCommand takeMoneyFromGroupCommand,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.takeMoneyFromGroupCommand = takeMoneyFromGroupCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TakeMoneyFromGroup command) {
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

        final var donateMoney = Money.from(command.amount().get());
        final var text = takeMoneyFromGroupCommand.execute(groupTg.domainGroupId(), user.personageId(), donateMoney)
            .fold(
                error -> switch (error) {
                    case TakeMoneyFromGroupError.NotEnoughMoney _ ->
                        GroupManagementLocalization.notEnoughMoneyForTake(groupTg.language());
                    case TakeMoneyFromGroupError.PersonageNotMember _ ->
                        GroupManagementLocalization.takeMoneyPersonageNotMember(groupTg.language());
                    case TakeMoneyFromGroupError.GroupNotRegistered _ ->
                        CommonLocalization.onlyForRegisteredGroup(groupTg.language());
                    case TakeMoneyFromGroupError.InvalidAmount _ ->
                        GroupManagementLocalization.incorrectAmount(groupTg.language());
                },
                personage ->
                    GroupManagementLocalization.successTakeMoney(groupTg.language(), personage, donateMoney)
            );
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(command.groupTgId())
                .text(text)
                .build()
        );
    }
}
