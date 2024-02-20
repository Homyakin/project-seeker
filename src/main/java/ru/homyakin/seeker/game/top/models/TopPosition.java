package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface TopPosition {
    PersonageId personageId();

    String personageBadgeWithName();

    int score();
}
