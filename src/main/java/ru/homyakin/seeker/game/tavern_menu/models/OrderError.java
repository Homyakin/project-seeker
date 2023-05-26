package ru.homyakin.seeker.game.tavern_menu.models;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;

public sealed interface OrderError {
    String text(Language language);

    record NotAvailableItem() implements OrderError {
        @Override
        public String text(Language language) {
            return TavernMenuLocalization.itemNotInMenu(language);
        }
    }

    record NotEnoughMoney(Category category, Money itemCost, Money personageMoney) implements OrderError {
        @Override
        public String text(Language language) {
            return switch (category) {
                case DRINK -> TavernMenuLocalization.notEnoughMoneyDrink(language, itemCost, personageMoney);
                case MAIN_DISH -> TavernMenuLocalization.notEnoughMoneyMainDish(language, itemCost, personageMoney);
            };
        }
    }

}
