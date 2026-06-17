package ru.homyakin.seeker.game.tavern_menu.order.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;

public sealed interface ConsumeOrderError {

    String text(Language language);

    enum WrongConsumer implements ConsumeOrderError {
        INSTANCE;
        @Override
        public String text(Language language) {
            return TavernMenuLocalization.wrongConsumer(language);
        }
    }

    enum AlreadyFinalStatus implements ConsumeOrderError {
        INSTANCE;
        @Override
        public String text(Language language) {
            return TavernMenuLocalization.consumeAlreadyInFinalStatus(language);
        }
    }

    enum OrderLocked implements ConsumeOrderError {
        INSTANCE;

        @Override
        public String text(Language language) {
            return TavernMenuLocalization.orderIsLocked(language);
        }
    }
}
