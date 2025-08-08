package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.Optional;

public record TopWorldRaidResearchPosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    Optional<String> tag,
    int contribution,
    Optional<Money> reward
) implements PersonageTopPosition {
    @Override
    public int score() {
        return contribution;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topWorldRaidResearchPosition(language, positionNumber, this);
    }
}
