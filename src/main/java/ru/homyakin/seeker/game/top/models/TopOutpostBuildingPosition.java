package ru.homyakin.seeker.game.top.models;

import java.util.Optional;

import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopOutpostBuildingPosition(
    PersonageId id,
    String personageName,
    BadgeView personageBadge,
    Optional<String> tag,
    int materials
) implements PersonageTopPosition {
    @Override
    public int score() {
        return materials;
    }

    @Override
    public String toLocalizedString(Language language, int positionNumber) {
        return TopLocalization.topOutpostBuildingPosition(language, positionNumber, this);
    }
}
