package ru.homyakin.seeker.game.item.models;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record Item(
    long id,
    ItemObject object,
    List<Modifier> modifiers,
    Optional<PersonageId> personageId,
    boolean isEquipped,
    Characteristics characteristics
) {
}
