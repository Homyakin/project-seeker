package ru.homyakin.seeker.game.top.models;

import java.util.List;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopOutpostBuildingResult(
    List<TopOutpostBuildingPosition> positions
) implements PersonageTopResult<TopOutpostBuildingPosition> {
    @Override
    public String toLocalizedString(Language language, PersonageId requestedId) {
        if (positions.isEmpty()) {
            return TopLocalization.topPersonageEmpty(language);
        }
        if (requestedId == null) {
            return TopLocalization.topOutpostBuilding(language, this);
        } else {
            return TopLocalization.topOutpostBuilding(language, requestedId, this);
        }
    }
}
