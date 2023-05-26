package ru.homyakin.seeker.game.tavern_menu.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;

public sealed interface ConsumeError {

    String text(Language language);

    enum WrongConsumer implements ConsumeError {
        INSTANCE;
        @Override
        public String text(Language language) {
            return TavernMenuLocalization.wrongConsumer(language);
        }
    }

    enum AlreadyFinalStatus implements ConsumeError {
        INSTANCE;
        @Override
        public String text(Language language) {
            return TavernMenuLocalization.consumeAlreadyInFinalStatus(language);
        }
    }
}
