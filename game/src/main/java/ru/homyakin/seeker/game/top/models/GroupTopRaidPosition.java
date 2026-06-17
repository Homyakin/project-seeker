package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

public record GroupTopRaidPosition(
    GroupId id,
    BadgeView badge,
    Optional<String> tag,
    String name,
    int successRaids,
    int failedRaids,
    int raidPoints
) implements GroupTopPosition {
    @Override
    public int score() {
        return raidPoints;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topGroupRaidPosition(language, positionNumber, this);
    }
}
