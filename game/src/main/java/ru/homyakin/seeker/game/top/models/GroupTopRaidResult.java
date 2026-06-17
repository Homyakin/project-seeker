package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.time.LocalDate;
import java.util.List;

public record GroupTopRaidResult(
    LocalDate startDate,
    LocalDate endDate,
    List<GroupTopRaidPosition> positions
) implements GroupTopResult<GroupTopRaidPosition> {
    @Override
    public String toLocalizedString(Language language, GroupId requestedId) {
        if (positions.isEmpty()) {
            return TopLocalization.topRaidEmpty(language);
        } else {
            return TopLocalization.topGroupRaidWeek(language, requestedId, this);
        }
    }
}
