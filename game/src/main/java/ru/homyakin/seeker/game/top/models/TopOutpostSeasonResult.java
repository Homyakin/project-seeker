package ru.homyakin.seeker.game.top.models;

import java.util.List;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopOutpostSeasonResult(
    TopOutpostBuildingResult inner,
    SeasonNumber seasonNumber
) implements PersonageTopResult<TopOutpostBuildingPosition> {
    @Override
    public List<TopOutpostBuildingPosition> positions() {
        return inner.positions();
    }

    @Override
    public String toLocalizedString(Language language, PersonageId requestedId) {
        if (inner.positions().isEmpty()) {
            return TopLocalization.topOutpostBuildingSeasonEmpty(language);
        }
        if (requestedId == null) {
            return TopLocalization.topOutpostBuildingSeasonGroup(language, this);
        }
        return TopLocalization.topOutpostBuildingSeasonGroup(language, requestedId, this);
    }
}
