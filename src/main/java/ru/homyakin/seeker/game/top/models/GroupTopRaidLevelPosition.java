package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

public record GroupTopRaidLevelPosition(
    GroupId id,
    BadgeView badge,
    Optional<String> tag,
    String name,
    int raidLevel
) implements GroupTopPosition {
    @Override
    public int score() {
        return raidLevel;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topGroupRaidLevelPosition(language, positionNumber, this);
    }

    @Override
    public String toLocalizedSelectedString(Language language, int positionNumber) {
        return TopLocalization.selectedPosition(language, toLocalizedString(language, positionNumber));
    }
}
