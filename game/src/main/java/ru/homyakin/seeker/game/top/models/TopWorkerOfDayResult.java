package ru.homyakin.seeker.game.top.models;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopWorkerOfDayResult(
    List<TopWorkerOfDayPosition> positions,
    Type type
) implements PersonageTopResult<TopWorkerOfDayPosition> {
    @Override
    public String toLocalizedString(Language language, PersonageId requestedPersonageId) {
        if (positions.isEmpty()) {
            return TopLocalization.topWorkerOfDayEmpty(language);
        } else {
            return switch (type) {
                case GROUP -> TopLocalization.topWorkerOfDayGroup(language, requestedPersonageId, this);
            };
        }
    }

    public enum Type {
        GROUP,
        ;
    }
}
