package ru.homyakin.seeker.game.top.models;

import java.util.List;
import java.util.Optional;

import ru.homyakin.seeker.locale.Language;

public interface TopResult<IdType, T extends TopPosition<IdType>> {
    List<T> positions();

    default Optional<Integer> findIdIndex(IdType id) {
        for (int i = 0; i < positions().size(); ++i) {
            if (positions().get(i).id().equals(id)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    String toLocalizedString(Language language, IdType requestedId);
}
