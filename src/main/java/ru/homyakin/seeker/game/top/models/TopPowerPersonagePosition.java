package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopPowerPersonagePosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    int power
) implements PersonageTopPosition {
    @Override
    public int score() {
        return power;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topPowerPersonagePosition(language, positionNumber, this);
    }
}
