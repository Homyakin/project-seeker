package ru.homyakin.seeker.game.top.models;

import java.time.LocalDate;
import java.util.List;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopRaidResult(
    LocalDate startDate,
    LocalDate endDate,
    List<TopRaidPosition> positions,
    Type type
) implements PersonageTopResult<TopRaidPosition> {
    @Override
    public String toLocalizedString(Language language, PersonageId requestedPersonageId) {
        if (positions.isEmpty()) {
            return TopLocalization.topRaidEmpty(language);
        } else {
            return switch (type) {
                case WEEK -> TopLocalization.topRaidWeek(language, requestedPersonageId, this);
                case WEEK_GROUP -> TopLocalization.topRaidWeekGroup(language, requestedPersonageId, this);
            };
        }
    }

    public enum Type {
        WEEK,
        WEEK_GROUP,
        ;
    }
}
