package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.List;

public record TopPowerPersonageResult(
    List<TopPowerPersonagePosition> positions
) implements PersonageTopResult<TopPowerPersonagePosition> {
    @Override
    public String toLocalizedString(Language language, PersonageId requestedPersonageId) {
        if (positions.isEmpty()) {
            return TopLocalization.topPersonageEmpty(language);
        } else {
            return TopLocalization.topPowerPersonageGroup(language, requestedPersonageId, this);
        }
    }
}
