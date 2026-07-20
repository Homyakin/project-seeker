package ru.homyakin.seeker.game.battle;

import java.util.Map;
import ru.homyakin.seeker.game.item.models.AttackType;

public record BattleRangeAttackInitSnapshot(
    int range,
    Map<AttackType, Integer> attack
) {
}
