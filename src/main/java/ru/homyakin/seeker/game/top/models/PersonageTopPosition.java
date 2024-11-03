package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface PersonageTopPosition extends TopPosition<PersonageId> {
    String personageName();

    BadgeView personageBadge();

    default String personageBadgeWithName() {
        return personageBadge().icon() + personageName();
    }
}
