package ru.homyakin.seeker.game.top.models;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

public record TopSpinResult(
    List<TopSpinPosition> positions,
    Type type
) implements TopResult<TopSpinPosition> {
    @Override
    public String toLocalizedString(Language language, PersonageId requestedPersonageId) {
        if (positions.isEmpty()) {
            return TopLocalization.topSpinEmpty(language);
        } else {
            return switch (type) {
                case GROUP -> TopLocalization.topSpinGroup(language, requestedPersonageId, this);
            };
        }
    }

    public enum Type {
        GROUP,
        ;
    }
}
