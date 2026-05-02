package ru.homyakin.seeker.game.battle.v4;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Nullable;
import ru.homyakin.seeker.utils.ProbabilityPicker;
import ru.homyakin.seeker.utils.RandomUtils;

public class BattlePersonage {
    private static final int TURN_INITIATIVE = 1000;
    private static final int THREAT_FROM_DAMAGE = 5;
    private static final int THREAT_LOSE_FROM_DAMAGE = 8;
    private static final int THREAT_FROM_KILL = THREAT_FROM_DAMAGE * 10;

    private final int rangePercent = 10;
    private final double defenseCoef = 500;

    private UUID id = UUID.randomUUID();
    private int health;
    private final int attack;
    private final int defense;
    private final int critChance;
    private final int dodgeChance;
    private final double critMultiplier;
    private final int initiative;
    private final int baseThreat;
    private int bonusThreat = 0;
    private int nextMove;
    private final Position startPosition;
    private int currentPosition = -1;
    private BattleAdvanceDirection advanceDirection;
    private final int range;

    private long normalDamageDealt;
    private long normalAttackCount;
    private long critDamageDealt;
    private long critsCount;
    private long damageBlocked;
    private long blockCount;
    private long damageDodged;
    private long dodgesCount;
    private long missesCount;

    public BattlePersonage(
        int health,
        int attack,
        int defense,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int initiative,
        int baseThreat,
        Position startPosition,
        int range
    ) {
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.critChance = critChance;
        this.dodgeChance = dodgeChance;
        this.critMultiplier = critMultiplier;
        this.initiative = initiative;
        this.baseThreat = baseThreat;
        this.startPosition = startPosition;
        this.range = range;
        this.nextMove = RandomUtils.getInInterval(0, TURN_INITIATIVE / 2);
    }

    /**
     * @return true if damage was applied (not dodged)
     */
    public boolean receiveDamageFrom(BattlePersonage attacker, DamageRoll roll) {
        if (RandomUtils.processChance(dodgeChance)) {
            dodgesCount++;
            damageDodged += roll.amount();
            attacker.recordMiss();
            return false;
        }
        bonusThreat = Math.max(0, bonusThreat - THREAT_LOSE_FROM_DAMAGE);
        attacker.recordDamageDealt(roll);
        damageBlocked += roll.amount();
        blockCount++;
        this.health = health - (int) (roll.amount() * defenseReduce());
        return true;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean tick() {
        nextMove += initiative;
        if (nextMove >= TURN_INITIATIVE) {
            nextMove -= TURN_INITIATIVE;
            return true;
        }
        return false;
    }

    /**
     * @return true if the mover phase should stop (no alive enemies left)
     */
    public boolean move(Map<UUID, BattlePersonage> enemyAliveTeam) {
        if (!isAlive()) {
            return false;
        }

        final var target = randomAlivePersonage(this, enemyAliveTeam);
        if (target == null) {
            stepOneLineTowardEnemy();
            return enemyAliveTeam.values().stream().noneMatch(BattlePersonage::isAlive);
        }
        if (target.receiveDamageFrom(this, rollDamage())) {
            if (!target.isAlive()) {
                bonusThreat += THREAT_FROM_KILL;
                enemyAliveTeam.remove(target.id());
            } else {
                bonusThreat += THREAT_FROM_DAMAGE;
            }
        }
        return false;
    }

    @Nullable
    private BattlePersonage randomAlivePersonage(BattlePersonage attacker, Map<UUID, BattlePersonage> enemyAliveTeam) {
        if (enemyAliveTeam.isEmpty()) {
            return null;
        }

        boolean hasAlive = false;
        for (final var personage : enemyAliveTeam.values()) {
            if (personage.isAlive()) {
                hasAlive = true;
                break;
            }
        }
        if (!hasAlive) {
            return null;
        }

        final var weightMap = new HashMap<BattlePersonage, Integer>();
        for (final var personage : enemyAliveTeam.values()) {
            if (!personage.isAlive()) {
                continue;
            }
            if (!inStrikeRange(attacker, personage)) {
                continue;
            }
            weightMap.put(personage, personage.totalThreat());
        }
        if (!weightMap.isEmpty()) {
            return new ProbabilityPicker<>(weightMap).pick(RandomUtils::getWithMax);
        }

        return null;
    }

    private static boolean inStrikeRange(BattlePersonage attacker, BattlePersonage target) {
        return Math.abs(attacker.currentPosition() - target.currentPosition()) <= attacker.range();
    }

    private void stepOneLineTowardEnemy() {
        currentPosition += advanceDirection.indexDelta();
    }

    public Position startPosition() {
        return startPosition;
    }

    public int currentPosition() {
        return currentPosition;
    }

    public int range() {
        return range;
    }

    public BattleAdvanceDirection advanceDirection() {
        return advanceDirection;
    }

    void placeOnBattlefield(int currentPosition, BattleAdvanceDirection advanceDirection) {
        this.currentPosition = currentPosition;
        this.advanceDirection = advanceDirection;
    }

    public UUID id() {
        return id;
    }

    public int totalThreat() {
        return baseThreat + bonusThreat;
    }

    public DamageRoll rollDamage() {
        if (RandomUtils.processChance(critChance)) {
            return new DamageRoll(
                RandomUtils.getInPercentRange((int) (attack * critMultiplier), rangePercent),
                true
            );
        }
        return new DamageRoll(RandomUtils.getInPercentRange(attack, rangePercent), false);
    }

    public BattlePersonageStats battlePersonageStats() {
        return new BattlePersonageStats(
            health,
            normalDamageDealt,
            normalAttackCount,
            critDamageDealt,
            critsCount,
            damageBlocked,
            blockCount,
            damageDodged,
            dodgesCount,
            missesCount
        );
    }

    private void recordMiss() {
        missesCount++;
    }

    private void recordDamageDealt(DamageRoll roll) {
        if (roll.crit()) {
            critDamageDealt += roll.amount();
            critsCount++;
        } else {
            normalDamageDealt += roll.amount();
            normalAttackCount++;
        }
    }

    public double power() {
        final var critProbability = critChance / 100.0;
        final var effectiveDamage = attack * (1 + critProbability * (critMultiplier - 1));
        final var dodgeProbability = dodgeChance / 100.0;
        return health * effectiveDamage / defenseReduce() / (1 - dodgeProbability) * ((double) initiative / TURN_INITIATIVE);
    }

    private double defenseReduce() {
        return (1 - (defense / (defense + defenseCoef)));
    }
}
