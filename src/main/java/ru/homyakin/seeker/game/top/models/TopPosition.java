package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.locale.Language;

public interface TopPosition<IdType> {
    IdType id();

    int score();

    String toLocalizedString(Language language, int positionNumber);
}
