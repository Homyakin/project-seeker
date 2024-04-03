package ru.homyakin.seeker.game.item.models;

import java.util.Optional;
import ru.homyakin.seeker.utils.models.DoubleRange;
import ru.homyakin.seeker.utils.models.IntRange;

public record ItemGenerateCharacteristics(
    Optional<IntRange> health,
    Optional<IntRange> attack,
    Optional<IntRange> defense,
    Optional<DoubleRange> multiplier
) {
}
