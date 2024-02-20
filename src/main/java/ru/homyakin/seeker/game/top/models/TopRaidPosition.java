package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record TopRaidPosition(
    PersonageId personageId,
    String personageName,
    BadgeView personageBadge,
    int successRaids,
    int failedRaids
) implements TopPosition {

    @Override
    public String personageBadgeWithName() {
        return personageBadge.icon() + personageName;
    }

    @Override
    public int score() {
        return successRaids * 2 + failedRaids;
    }
}
