package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.PersonageTopPosition;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

public record TopDonatePosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    Optional<String> tag,
    long donateMoney
) implements PersonageTopPosition {
    @Override
    public int score() {
        return (int) donateMoney;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topDonatePosition(language, positionNumber, this);
    }

    @Override
    public String toLocalizedSelectedString(Language language, int positionNumber) {
        return TopLocalization.selectedPosition(language, toLocalizedString(language, positionNumber));
    }
}
