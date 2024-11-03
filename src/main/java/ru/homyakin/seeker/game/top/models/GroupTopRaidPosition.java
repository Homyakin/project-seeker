package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record GroupTopRaidPosition(
    GroupId id,
    String name,
    int successRaids,
    int failedRaids
) implements GroupTopPosition {
    @Override
    public int score() {
        return successRaids * 2 + failedRaids;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topGroupRaidPosition(language, positionNumber, this);
    }
}
