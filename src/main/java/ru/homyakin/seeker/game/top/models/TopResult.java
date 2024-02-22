package ru.homyakin.seeker.game.top.models;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;

public interface TopResult<T extends TopPosition> {
    List<T> positions();

    default Optional<Integer> findPersonageIndex(PersonageId personageId) {
        for (int i = 0; i < positions().size(); ++i) {
            if (positions().get(i).personageId().equals(personageId)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    String toLocalizedString(Language language, PersonageId requestedPersonageId);
}
