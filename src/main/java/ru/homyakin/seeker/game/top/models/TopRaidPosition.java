package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

public record TopRaidPosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    Optional<String> tag,
    int successRaids,
    int failedRaids,
    int raidPoints
) implements PersonageTopPosition {
    @Override
    public int score() {
        return raidPoints;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topRaidPosition(language, positionNumber, this);
    }
}
