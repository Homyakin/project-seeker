package ru.homyakin.seeker.game.utils;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;

public sealed interface NameError {

    String toUserMessage(Language language);

    record InvalidLength(int minLength, int maxLength) implements NameError {
        @Override
        public String toUserMessage(Language language) {
            return ChangeNameLocalization.personageNameInvalidLength(language, minLength, maxLength);
        }
    }

    record NotAllowedSymbols() implements NameError {
        @Override
        public String toUserMessage(Language language) {
            return ChangeNameLocalization.personageNameInvalidSymbols(language);
        }
    }
}
