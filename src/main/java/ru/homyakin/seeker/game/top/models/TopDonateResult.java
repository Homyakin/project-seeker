package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.List;

public record TopDonateResult(
    List<TopDonatePosition> positions,
    SeasonNumber seasonNumber
) implements PersonageTopResult<TopDonatePosition> {

    @Override
    public String toLocalizedString(Language language, PersonageId requestedId) {
        if (positions.isEmpty()) {
            return TopLocalization.topDonateEmpty(language);
        } else {
            return TopLocalization.topDonateGroup(language, requestedId, this);
        }
    }
} 
