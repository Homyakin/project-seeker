package ru.homyakin.seeker.game.top.models;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopRaidResult(
    LocalDate startDate,
    LocalDate endDate,
    List<TopRaidPosition> positions
) implements TopResult {
    @Override
    public String toLocalizedString(Language language, PersonageId requestedPersonageId) {
        if (positions.isEmpty()) {
            return TopLocalization.topRaidEmpty(language);
        } else {
            return TopLocalization.topRaidWeek(language, requestedPersonageId, this);
        }
    }

    public Optional<Integer> findPersonageIndex(PersonageId personageId) {
        for (int i = 0; i < positions.size(); ++i) {
            if (positions.get(i).personageId().equals(personageId)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }
}
