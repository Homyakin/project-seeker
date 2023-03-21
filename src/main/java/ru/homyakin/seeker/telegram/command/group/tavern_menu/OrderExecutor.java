package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.errors.OrderError;
import ru.homyakin.seeker.game.tavern_menu.MenuService;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupStatsService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class OrderExecutor extends CommandExecutor<Order> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final MenuService menuService;
    private final TelegramSender telegramSender;
    private final GroupStatsService groupStatsService;

    public OrderExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        MenuService menuService,
        TelegramSender telegramSender,
        GroupStatsService groupStatsService
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.menuService = menuService;
        this.telegramSender = telegramSender;
        this.groupStatsService = groupStatsService;
    }

    @Override
    public void execute(Order command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUser.first();
        if (command.itemId().isEmpty()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    group.id(),
                    TavernMenuLocalization.itemNotInMenu(group.language()),
                    command.messageId()
                )
            );
            return;
        }
        final var menuItemResult = menuService.getAvailableMenuItem(command.itemId().get());
        if (menuItemResult.isEmpty()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    group.id(),
                    TavernMenuLocalization.itemNotInMenu(group.language()),
                    command.messageId()
                )
            );
            return;
        }
        final var menuItem = menuItemResult.get();
        final var personage = personageService.getByIdForce(groupUser.second().personageId());
        final var result = personageService.orderMenuItem(personage, menuItem);
        if (result.isLeft()) {
            final var error = result.getLeft();
            final String message;
            if (error instanceof OrderError.NotAvailableItem) {
                message = TavernMenuLocalization.itemNotInMenu(group.language());
            } else if (error instanceof OrderError.NotEnoughMoney notEnoughMoney) {
                message = switch (menuItem.category()) {
                    case DRINK -> TavernMenuLocalization.notEnoughMoneyDrink(
                        group.language(), notEnoughMoney.itemCost(), notEnoughMoney.personageMoney()
                    );
                    case MAIN_DISH -> TavernMenuLocalization.notEnoughMoneyMainDish(
                        group.language(), notEnoughMoney.itemCost(), notEnoughMoney.personageMoney()
                    );
                };
            } else {
                throw new IllegalStateException("Unknown error " + error.toString());
            }
            telegramSender.send(
                TelegramMethods.createSendMessage(group.id(), message, command.messageId())
            );
        } else {
            groupStatsService.increaseTavernMoneySpent(group.id(), menuItem.price().value());
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    group.id(),
                    menuItem.orderText(group.language(), personage),
                    command.messageId()
                )
            );
        }
    }

}
