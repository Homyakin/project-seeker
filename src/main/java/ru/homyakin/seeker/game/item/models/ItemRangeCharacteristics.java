package ru.homyakin.seeker.game.item.models;

import java.util.Optional;
import ru.homyakin.seeker.utils.models.IntRange;

public record ItemRangeCharacteristics(
    Optional<IntRange> attack
) {
}
