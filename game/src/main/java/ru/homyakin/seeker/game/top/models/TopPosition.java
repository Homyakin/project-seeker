package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public interface TopPosition<IdType> {
    IdType id();

    int score();

    String toLocalizedString(Language language, int positionNumber);

    default String toLocalizedSelectedString(Language language, int positionNumber) {
        return TopLocalization.selectedPosition(
            language,
            toLocalizedString(language, positionNumber)
        );
    }
}
