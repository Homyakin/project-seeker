package ru.homyakin.seeker.game.top.models;

import java.util.List;

import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopOutpostBuildSessionResult(
    TopOutpostBuildingResult inner,
    Building building,
    int targetLevel
) implements PersonageTopResult<TopOutpostBuildingPosition> {
    @Override
    public List<TopOutpostBuildingPosition> positions() {
        return inner.positions();
    }

    @Override
    public String toLocalizedString(Language language, PersonageId requestedId) {
        if (inner.positions().isEmpty()) {
            return TopLocalization.topOutpostBuildSessionEmpty(language);
        }
        if (requestedId == null) {
            return TopLocalization.topOutpostBuildSessionGroup(language, this);
        }
        return TopLocalization.topOutpostBuildSessionGroup(language, requestedId, this);
    }
}
