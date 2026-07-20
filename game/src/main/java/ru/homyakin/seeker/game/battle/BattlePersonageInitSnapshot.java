package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;

public record BattlePersonageInitSnapshot(
    UUID id,
    Optional<String> name,
    boolean firstTeam,
    int health,
    int lineIndex,
    BattleAdvanceDirection advanceDirection,
    int initiative,
    int initiativeGauge,
    int range,
    int totalThreat,
    List<BattleItemInitSnapshot> items,
    List<BattleSkillInitSnapshot> skills,
    int critChance,
    int dodgeChance,
    double critMultiplier,
    List<BattleRangeAttackInitSnapshot> attacksByRange,
    Map<DefenseType, Integer> defenses,
    Map<AttackType, Double> damageTakenMultipliers
) {
}
