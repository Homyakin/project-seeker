package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.DonateToGroupCommand;
import ru.homyakin.seeker.game.group.error.DonateMoneyToGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class DonateToGroupExecutor extends CommandExecutor<DonateToGroup> {
    private final GroupUserService groupUserService;
    private final DonateToGroupCommand donateToGroupCommand;
    private final TelegramSender telegramSender;

    public DonateToGroupExecutor(
        GroupUserService groupUserService,
        DonateToGroupCommand donateToGroupCommand,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.donateToGroupCommand = donateToGroupCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(DonateToGroup command) {
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
        final var text = donateToGroupCommand.execute(groupTg.domainGroupId(), user.personageId(), donateMoney)
            .fold(
                error -> switch (error) {
                    case DonateMoneyToGroupError.InvalidAmount _ ->
                        GroupManagementLocalization.incorrectAmount(groupTg.language());
                    case DonateMoneyToGroupError.NotEnoughMoney _ ->
                        GroupManagementLocalization.notEnoughMoneyForDonate(groupTg.language());
                },
                personage ->
                    GroupManagementLocalization.successDonate(groupTg.language(), personage, donateMoney)
            );
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(command.groupTgId())
                .text(text)
                .build()
        );
    }
}
