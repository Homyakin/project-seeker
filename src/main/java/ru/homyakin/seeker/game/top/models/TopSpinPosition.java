package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

public record TopSpinPosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    Optional<String> tag,
    int workCount
) implements PersonageTopPosition {
    @Override
    public int score() {
        return workCount;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topSpinPosition(language, positionNumber, this);
    }
}
