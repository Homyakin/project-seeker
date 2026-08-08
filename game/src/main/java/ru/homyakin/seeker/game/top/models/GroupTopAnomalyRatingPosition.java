package ru.homyakin.seeker.game.top.models;

import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record GroupTopAnomalyRatingPosition(
    GroupId id,
    BadgeView badge,
    Optional<String> tag,
    String name,
    int rating
) implements GroupTopPosition {
    @Override
    public int score() {
        return rating;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topGroupAnomalyRatingPosition(language, positionNumber, this);
    }
}
