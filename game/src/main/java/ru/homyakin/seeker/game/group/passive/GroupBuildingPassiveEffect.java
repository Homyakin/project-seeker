package ru.homyakin.seeker.game.group.passive;

import java.time.LocalDateTime;
import java.util.Optional;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.outpost.entity.Building;

public record GroupBuildingPassiveEffect(
    Building building,
    Effect effect,
    Optional<LocalDateTime> expiresAt
) implements GroupPassiveEffect {
}
