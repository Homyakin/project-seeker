package ru.homyakin.seeker.game.tavern_menu.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;

public sealed interface MenuItemOrderError {

    String text(Language language);

    enum WrongConsumer implements MenuItemOrderError {
        INSTANCE;
        @Override
        public String text(Language language) {
            return TavernMenuLocalization.wrongConsumer(language);
        }
    }

    enum AlreadyFinalStatus implements MenuItemOrderError {
        INSTANCE;
        @Override
        public String text(Language language) {
            return TavernMenuLocalization.consumeAlreadyInFinalStatus(language);
        }
    }

    enum OrderLocked implements MenuItemOrderError {
        INSTANCE;

        @Override
        public String text(Language language) {
            return TavernMenuLocalization.orderIsLocked(language);
        }
    }
}
