package ru.homyakin.seeker.game.top.models;

import java.util.List;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record GroupTopAnomalyRatingResult(
    List<GroupTopAnomalyRatingPosition> positions
) implements GroupTopResult<GroupTopAnomalyRatingPosition> {
    @Override
    public String toLocalizedString(Language language, GroupId requestedId) {
        if (positions.isEmpty()) {
            return TopLocalization.topAnomalyRatingEmpty(language);
        } else {
            return TopLocalization.topGroupAnomalyRating(language, requestedId, this);
        }
    }
}
