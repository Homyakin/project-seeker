package ru.homyakin.seeker.game.item.models;

import java.util.List;
import java.util.Optional;

public record Item(
    long id,
    ItemObject object,
    List<Modifier> modifiers,
    Optional<Long> personageId,
    boolean isEquipped,
    int attack
) {
}
