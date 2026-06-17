package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

public interface PersonageTopPosition extends TopPosition<PersonageId> {
    String personageName();

    BadgeView personageBadge();

    Optional<String> tag();
}
