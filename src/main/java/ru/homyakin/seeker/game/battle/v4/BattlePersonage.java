package ru.homyakin.seeker.game.battle.v4;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ru.homyakin.seeker.game.battle.v4.skill.PassiveSkill;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.ProbabilityPicker;
import ru.homyakin.seeker.utils.RandomUtils;

public class BattlePersonage {
    private static final int REQUIRED_SPEED = 1000;
    private static final int THREAT_FROM_DAMAGE = 5;
    private static final int THREAT_LOSE_FROM_DAMAGE = 8;
    private static final int THREAT_FROM_KILL = THREAT_FROM_DAMAGE * 10;
    private static final int RANGE_PERCENT = 10;
    private static final double DEFENSE_COEF = 500;

    private static final Map<DefenseType, Map<AttackType, Double>> DAMAGE_MATRIX = Map.of(
        DefenseType.CLOTH, Map.of(
            AttackType.SLASH, 0.75,
            AttackType.BLUNT, 1.2,
            AttackType.PIERCE, 0.8,
            AttackType.MAGICAL, 1.25
        ),
        DefenseType.LEATHER, Map.of(
            AttackType.SLASH, 0.8,
            AttackType.BLUNT, 1.2,
            AttackType.PIERCE, 1.25,
            AttackType.MAGICAL, 0.75
        ),
        DefenseType.PLATE, Map.of(
            AttackType.SLASH, 1.25,
            AttackType.BLUNT, 0.75,
            AttackType.PIERCE, 1.2,
            AttackType.MAGICAL, 0.8
        ),
        DefenseType.ARCANE, Map.of(
            AttackType.SLASH, 1.2,
            AttackType.BLUNT, 0.85,
            AttackType.PIERCE, 0.75,
            AttackType.MAGICAL, 1.20
        )
    );

    private final UUID id = UUID.randomUUID();
    private int health;
    private final Map<AttackType, Integer>[] rangeAttack;
    private final Map<AttackType, Integer>[] rangeAttackCrit;
    private final Map<DefenseType, Integer> defense;
    private final Map<AttackType, Double> defenseReduce;
    private final int critChance;
    private final int dodgeChance;
    private final double critMultiplier;
    private final int speed;
    private final int baseThreat;
    private int bonusThreat = 0;
    private int cumulativeSpeed;
    private final Position startPosition;
    private int currentPosition = -1;
    private BattleAdvanceDirection advanceDirection;
    private final int range;

    private long normalDamageDealt;
    private long normalAttackCount;
    private long critDamageDealt;
    private long critsCount;
    private long damageBlocked;
    private long actualDamageTaken;
    private long blockCount;
    private long damageDodged;
    private long dodgesCount;
    private long missesCount;

    public BattlePersonage(
        List<Item> items,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat,
        Position startPosition
    ) {
        this.defense = new EnumMap<>(DefenseType.class);
        int percentAttack = 0;
        int percentDefence = 0;
        int percentCrit = 0;
        int percentDodge = 0;
        int percentSpeed = 0;
        var maxRange = 1;
        for (final var item: items) {
            if (item.itemAttack().isPresent()) {
                maxRange = Math.max(maxRange, item.itemAttack().get().range());
            }
            for (final var skill: item.itemSkills()) {
                if (skill instanceof PassiveSkill(int attack, int defence, int crit, int dodge, int bonusSpeed)) {
                    percentAttack += attack;
                    percentDefence += defence;
                    percentCrit += crit;
                    percentDodge += dodge;
                    percentSpeed += bonusSpeed;
                }
            }
        }

        this.rangeAttack = newRangeAttackSlotMaps(maxRange);
        for (final var item : items) {
            if (item.itemAttack().isEmpty()) {
                final var attack = item.itemAttack().get();
                for (int i = 1; i <= attack.range(); i++) {
                    rangeAttack[i].merge(
                        attack.attackType(),
                        MathUtils.addPercent(attack.attack(), percentAttack),
                        Integer::sum
                    );
                }
            }
            if (item.itemDefense().isPresent()) {
                defense.merge(
                    item.itemDefense().get().defenseType(),
                    MathUtils.addPercent(item.itemDefense().get().defense(), percentDefence),
                    Integer::sum
                );
            }
            this.health += item.health();
        }
        this.rangeAttackCrit = newRangeAttackSlotMaps(maxRange);
        for (int i = 1; i <= maxRange; i++) {
            for (var entry : rangeAttack[i].entrySet()) {
                rangeAttackCrit[i].put(entry.getKey(), MathUtils.addPercent(entry.getValue(), percentCrit));
            }
        }
        this.defenseReduce = new EnumMap<>(AttackType.class);
        for (AttackType attackType : AttackType.values()) {
            double effectiveDef = 0;
            for (var entry : defense.entrySet()) {
                double multiplier = DAMAGE_MATRIX.get(entry.getKey()).get(attackType);
                effectiveDef += entry.getValue() * multiplier;
            }
            defenseReduce.put(
                attackType,
                1 - (effectiveDef / (effectiveDef + DEFENSE_COEF))
            );
        }
        this.critChance = critChance;
        this.dodgeChance = MathUtils.addPercent(dodgeChance, percentDodge);
        this.critMultiplier = critMultiplier;
        this.speed = MathUtils.addPercent(speed, percentSpeed);
        this.baseThreat = baseThreat;
        this.startPosition = startPosition;
        this.range = maxRange;
        this.cumulativeSpeed = RandomUtils.getInInterval(0, REQUIRED_SPEED / 2);
    }

    @SuppressWarnings("unchecked")
    private static Map<AttackType, Integer>[] newRangeAttackSlotMaps(int maxSlot) {
        final Map<AttackType, Integer>[] maps = new Map[maxSlot + 1];
        for (int i = 1; i <= maxSlot; i++) {
            maps[i] = new EnumMap<>(AttackType.class);
        }
        return maps;
    }

    /**
     * @return true if damage was applied (not dodged)
     */
    public boolean receiveDamageFrom(BattlePersonage attacker, DamageRoll roll, BattleActionLog log, int round) {
        if (RandomUtils.processChance(dodgeChance)) {
            dodgesCount++;
            damageDodged += roll.amount();
            attacker.recordMiss();
            log.add(new BattleEvent.AttackDodged(attacker.id(), id, round));
            return false;
        }
        bonusThreat = Math.max(0, bonusThreat - THREAT_LOSE_FROM_DAMAGE);
        damageBlocked += roll.amount();
        blockCount++;
        final int healthBefore = health;
        int totalDamage = 0;
        for (var entry : roll.attack().entrySet()) {
            totalDamage += (int) (entry.getValue() * defenseReduce.get(entry.getKey()));
        }
        totalDamage = RandomUtils.getInPercentRange(totalDamage, RANGE_PERCENT);
        attacker.recordDamageDealt(totalDamage, roll.crit());
        actualDamageTaken += totalDamage;
        this.health = health - totalDamage;
        final int damageTaken = healthBefore - health;
        log.add(new BattleEvent.DamageReceived(id, attacker.id(), roll, damageTaken, health, round));
        return true;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean tick(BattleActionLog log, int round) {
        cumulativeSpeed += speed;
        if (cumulativeSpeed >= REQUIRED_SPEED) {
            cumulativeSpeed -= REQUIRED_SPEED;
            log.add(new BattleEvent.InitiativeAfterTick(id, cumulativeSpeed, true, round));
            return true;
        }
        log.add(new BattleEvent.InitiativeAfterTick(id, cumulativeSpeed, false, round));
        return false;
    }

    /**
     * @return true if the mover phase should stop (no alive enemies left)
     */
    public boolean move(Map<UUID, BattlePersonage> enemyAliveTeam, BattleActionLog log, int round) {
        if (!isAlive()) {
            return false;
        }

        // поддержка списка сразу на будущее
        final var targets = randomAlivePersonage(this, enemyAliveTeam);
        if (targets.isEmpty()) {
            stepOneLineTowardEnemy();
            log.add(new BattleEvent.MovedTowardEnemy(id, currentPosition, round));
            return enemyAliveTeam.values().stream().noneMatch(BattlePersonage::isAlive);
        }
        final var target = targets.getFirst();
        final var personage = target.personage;
        if (personage.receiveDamageFrom(this, rollDamage(target.range), log, round)) {
            if (!personage.isAlive()) {
                log.add(new BattleEvent.PersonageDefeated(personage.id(), id, round));
                bonusThreat += THREAT_FROM_KILL;
                enemyAliveTeam.remove(personage.id());
            } else {
                bonusThreat += THREAT_FROM_DAMAGE;
            }
        }
        return false;
    }

    private List<Target> randomAlivePersonage(BattlePersonage attacker, Map<UUID, BattlePersonage> enemyAliveTeam) {
        if (enemyAliveTeam.isEmpty()) {
            return List.of();
        }

        boolean hasAlive = false;
        for (final var personage : enemyAliveTeam.values()) {
            if (personage.isAlive()) {
                hasAlive = true;
                break;
            }
        }
        if (!hasAlive) {
            return List.of();
        }

        final var weightMap = new HashMap<BattlePersonage, Integer>();
        for (final var personage : enemyAliveTeam.values()) {
            if (!personage.isAlive()) {
                continue;
            }
            if (!attacker.inStrikeRange(personage)) {
                continue;
            }
            weightMap.put(personage, personage.totalThreat());
        }
        if (!weightMap.isEmpty()) {
            var target = new ProbabilityPicker<>(weightMap).pick(RandomUtils::getWithMax);
            return List.of(new Target(attacker.calcRange(target), target));
        }

        return List.of();
    }

    private boolean inStrikeRange(BattlePersonage target) {
        return calcRange(target) <= range();
    }

    private int calcRange(BattlePersonage target) {
        return Math.abs(currentPosition - target.currentPosition());
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

    public int health() {
        return health;
    }

    public int initiative() {
        return speed;
    }

    /**
     * Current initiative / turn gauge value (accumulates each tick until a turn is granted, then wraps).
     */
    public int initiativeGauge() {
        return cumulativeSpeed;
    }

    public int totalThreat() {
        return baseThreat + bonusThreat;
    }

    public DamageRoll rollDamage(int range) {
        if (RandomUtils.processChance(critChance)) {
            return new DamageRoll(rangeAttackCrit[range], true);
        }
        return new DamageRoll(rangeAttack[range], false);
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

    private void recordDamageDealt(int amount, boolean crit) {
        if (crit) {
            critDamageDealt += amount;
            critsCount++;
        } else {
            normalDamageDealt += amount;
            normalAttackCount++;
        }
    }

    public double power() {
        final var attack = rangeAttack[1].values().stream()
            .mapToInt(i -> i)
            .sum();
        final var totalDefense = defense.values().stream()
            .mapToInt(i -> i)
            .sum();
        final var defenseReduce = (1 - (totalDefense / (totalDefense + DEFENSE_COEF)));
        final var critProbability = critChance / 100.0;
        final var effectiveDamage = attack * (1 + critProbability * (critMultiplier - 1));
        final var dodgeProbability = dodgeChance / 100.0;
        return health * effectiveDamage / defenseReduce / (1 - dodgeProbability) * ((double) speed / REQUIRED_SPEED);
    }

    private record Target(
       int range,
       BattlePersonage personage
    ){ }
}
