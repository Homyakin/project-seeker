package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

/**
 * Read-only ranking position. Power is already converted to the active presentation scale.
 */
public record TopPowerPersonagePosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    Optional<String> tag,
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
