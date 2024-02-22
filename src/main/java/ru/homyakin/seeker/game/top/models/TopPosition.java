package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;

public interface TopPosition {
    PersonageId personageId();

    String personageBadgeWithName();

    int score();

    String toLocalizedString(Language language, int positionNumber);
}
