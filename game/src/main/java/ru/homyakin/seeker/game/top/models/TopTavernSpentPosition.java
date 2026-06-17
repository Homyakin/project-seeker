package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

public record TopTavernSpentPosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    Optional<String> tag,
    long tavernMoneySpent
) implements PersonageTopPosition {
    @Override
    public int score() {
        return (int) tavernMoneySpent;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topTavernSpentPosition(language, positionNumber, this);
    }
}
