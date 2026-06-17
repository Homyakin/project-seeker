package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.List;

public record TopWorldRaidResearchResult(
    List<TopWorldRaidResearchPosition> positions
) implements PersonageTopResult<TopWorldRaidResearchPosition> {

    @Override
    public String toLocalizedString(Language language, PersonageId requestedId) {
        if (positions.isEmpty()) {
            return TopLocalization.topWorldRaidResearchEmpty(language);
        } else {
            return TopLocalization.topWorldRaidResearch(language, requestedId, this);
        }
    }
}
