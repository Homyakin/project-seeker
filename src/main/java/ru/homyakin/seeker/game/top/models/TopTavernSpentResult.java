package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.List;

public record TopTavernSpentResult(
    List<TopTavernSpentPosition> positions,
    SeasonNumber seasonNumber
) implements PersonageTopResult<TopTavernSpentPosition> {

    @Override
    public String toLocalizedString(Language language, PersonageId requestedId) {
        if (positions.isEmpty()) {
            return TopLocalization.topTavernSpentEmpty(language);
        } else {
            return TopLocalization.topTavernSpentGroup(language, requestedId, this);
        }
    }
}
