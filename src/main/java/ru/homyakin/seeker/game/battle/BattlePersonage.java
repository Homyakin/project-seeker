package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidPersonage;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ru.homyakin.seeker.game.battle.effect.PeriodicDamageEffect;
import ru.homyakin.seeker.game.battle.effect.PersonageBattleEffects;
import ru.homyakin.seeker.game.battle.effect.TemporaryMaxRangeBonus;
import ru.homyakin.seeker.game.battle.skill.AttackPowerSkill;
import ru.homyakin.seeker.game.battle.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.skill.HealthPowerSkill;
import ru.homyakin.seeker.game.battle.skill.ItemSkill;
import ru.homyakin.seeker.game.battle.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.skill.TurnSkill;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.active_impl.SkillMapper;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.ProbabilityPicker;
import ru.homyakin.seeker.utils.RandomUtils;

public class BattlePersonage {
    private static final double BASE_CRIT_MULTIPLIER = 1.2;
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

    /**
     * Effective defense weight for {@code defenseType} against {@code attackType} (see {@link #refreshDefenseReduce()}).
     * Package-private for tests in this package.
     */
    static double damageMitigationMultiplier(DefenseType defenseType, AttackType attackType) {
        return DAMAGE_MATRIX.get(defenseType).get(attackType);
    }

    private final UUID id = UUID.randomUUID();
    private final int maxHealth;
    private int health;
    private final Set<AttackType> attackTypes = new HashSet<>();
    private final List<ItemSkill> itemSkills = new ArrayList<>();
    private final Map<AttackType, Integer>[] rangeAttack;
    private final Map<AttackType, Integer>[] rangeAttackCrit;
    private final Map<DefenseType, Integer> defense;
    private final Map<AttackType, Double> defenseReduce;
    private final List<TurnSkill.TurnStartSkill> turnStartSkills = new ArrayList<>();
    private final List<TurnSkill.TurnEndSkill> turnEndSkills = new ArrayList<>();
    private final List<DamageDealSkill.OnHitSkill> onHitSkills = new ArrayList<>();
    private final List<DamageDealSkill.OnCritSkill> onCritSkills = new ArrayList<>();
    private final List<DamageDealSkill.OnMissSkill> onMissSkills = new ArrayList<>();
    private final List<DamageDealSkill.OnDamageReceiveSkill> onDamageReceiveSkills = new ArrayList<>();
    private final List<DamageDealSkill.OnCritReceiveSkill> onCritReceiveSkills = new ArrayList<>();
    private final List<DamageDealSkill.OnDodgeSkill> onDodgeSkills = new ArrayList<>();
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
    private final int baseMaxRange;
    private final PersonageBattleEffects combatEffects = new PersonageBattleEffects();

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

    public BattlePersonage(List<Item> items, Position startPosition) {
        this(items, startPosition, Map.of());
    }

    public BattlePersonage(WorldRaidPersonage personage, Position startPosition) {
        this(itemsFromWorldRaidPersonage(personage), startPosition, skillPointsFromWorldRaidPersonage(personage));
    }

    public BattlePersonage(List<Item> items, Position startPosition, Map<ActiveEnum, Integer> skillPointsByActive) {
        this.defense = new EnumMap<>(DefenseType.class);
        var maxRange = 1;
        var activeSkills = new HashMap<ActiveEnum, Integer>();
        var totalCritChance = 0;
        var totalDodgeChance = 0;
        var totalCritMultiplier = BASE_CRIT_MULTIPLIER;
        var totalSpeed = 0;
        var totalBaseThreat = 0;
        for (final var item : items) {
            totalCritChance += item.critChance();
            totalDodgeChance += item.dodgeChance();
            totalCritMultiplier += item.critMultiplier();
            totalSpeed += item.speed();
            totalBaseThreat += item.baseThreat();
            if (item.itemAttack().isPresent()) {
                maxRange = Math.max(maxRange, item.itemAttack().get().range());
            }
            if (item.modifier().isPresent() && item.rarity() != ItemRarity.COMMON) {
                activeSkills.merge(item.modifier().get().activeEnum(), item.skillPoints(), Integer::sum);
            }
        }
        for (final var entry : skillPointsByActive.entrySet()) {
            activeSkills.put(entry.getKey(), activeSkills.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
        this.critMultiplier = totalCritMultiplier;
        for (final var entry : activeSkills.entrySet()) {
            itemSkills.add(SkillMapper.map(entry.getKey(), entry.getValue()));
        }
        for (final var skill : itemSkills) {
            switch (skill) {
                case TurnSkill.TurnStartSkill turnStartSkill -> turnStartSkills.add(turnStartSkill);
                case TurnSkill.TurnEndSkill turnEndSkill -> turnEndSkills.add(turnEndSkill);
                case DamageDealSkill.OnHitSkill onHitSkill -> onHitSkills.add(onHitSkill);
                case DamageDealSkill.OnCritSkill onCritSkill -> onCritSkills.add(onCritSkill);
                case DamageDealSkill.OnMissSkill onMissSkill -> onMissSkills.add(onMissSkill);
                case DamageDealSkill.OnDamageReceiveSkill onDamageReceiveSkill ->
                    onDamageReceiveSkills.add(onDamageReceiveSkill);
                case DamageDealSkill.OnCritReceiveSkill onCritReceiveSkill ->
                    onCritReceiveSkills.add(onCritReceiveSkill);
                case DamageDealSkill.OnDodgeSkill onDodgeSkill -> onDodgeSkills.add(onDodgeSkill);
            }
        }

        this.rangeAttack = newRangeAttackSlotMaps(maxRange);
        for (final var item : items) {
            if (item.itemAttack().isPresent()) {
                final var attack = item.itemAttack().get();
                attackTypes.add(attack.attackType());
                for (int i = 1; i <= attack.range(); i++) {
                    rangeAttack[i].merge(
                        attack.attackType(),
                        attack.attack(),
                        Integer::sum
                    );
                }
            }
            if (item.itemDefense().isPresent()) {
                defense.merge(
                    item.itemDefense().get().defenseType(),
                    item.itemDefense().get().defense(),
                    Integer::sum
                );
            }
            this.health += item.health();
        }
        this.rangeAttackCrit = newRangeAttackSlotMaps(maxRange);
        for (int i = 1; i <= maxRange; i++) {
            for (var entry : rangeAttack[i].entrySet()) {
                rangeAttackCrit[i].put(entry.getKey(), (int) (entry.getValue() * this.critMultiplier));
            }
        }
        this.defenseReduce = new EnumMap<>(AttackType.class);
        refreshDefenseReduce();
        this.critChance = totalCritChance;
        this.dodgeChance = totalDodgeChance;
        this.speed = totalSpeed;
        this.baseThreat = totalBaseThreat;
        this.startPosition = startPosition;
        this.baseMaxRange = maxRange;
        this.cumulativeSpeed = RandomUtils.getInInterval(0, REQUIRED_SPEED / 2);
        this.maxHealth = this.health;
    }

    private static List<Item> itemsFromWorldRaidPersonage(WorldRaidPersonage personage) {
        final var items = new ArrayList<Item>();
        items.add(new Item(
            new ItemObject(
                "world_raid_personage",
                Set.of(PersonageSlot.MAIN_HAND),
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                personage.health(),
                personage.critChance(),
                personage.dodgeChance(),
                personage.critMultiplier(),
                personage.speed(),
                personage.baseThreat(),
                Map.of()
            ),
            java.util.Optional.empty(),
            ItemRarity.COMMON
        ));
        for (final var attack : personage.attacksOrEmpty()) {
            items.add(new Item(
                new ItemObject(
                    "world_raid_attack",
                    Set.of(PersonageSlot.MAIN_HAND),
                    java.util.Optional.of(new ItemAttack(attack.attackType(), attack.range(), attack.attack())),
                    java.util.Optional.empty(),
                    0,
                    0,
                    0,
                    0.0,
                    0,
                    0,
                    Map.of()
                ),
                java.util.Optional.empty(),
                ItemRarity.COMMON
            ));
        }
        for (final var defense : personage.defensesOrEmpty()) {
            items.add(new Item(
                new ItemObject(
                    "world_raid_defense",
                    Set.of(PersonageSlot.BODY),
                    java.util.Optional.empty(),
                    java.util.Optional.of(new ItemDefense(defense.defenseType(), defense.defense())),
                    0,
                    0,
                    0,
                    0.0,
                    0,
                    0,
                    Map.of()
                ),
                java.util.Optional.empty(),
                ItemRarity.COMMON
            ));
        }
        return items;
    }

    private static Map<ActiveEnum, Integer> skillPointsFromWorldRaidPersonage(WorldRaidPersonage personage) {
        final var points = new HashMap<ActiveEnum, Integer>();
        for (final var skill : personage.skillsOrEmpty()) {
            points.put(skill.activeEnum(), points.getOrDefault(skill.activeEnum(), 0) + skill.rank().requiredPoints());
        }
        return points;
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
    public boolean receiveDamageFrom(BattlePersonage attacker, DamageRoll roll, BattleActionLog log, int round, BattleContext context) {
        if (RandomUtils.processChance(dodgeChance)) {
            dodgesCount++;
            damageDodged += roll.amount();
            attacker.recordMiss();
            log.add(new BattleEvent.AttackDodged(attacker.id(), id, round));
            log.addAll(applyOnDodgeSkills(context, attacker, round));
            if (attacker.isAlive()) {
                log.addAll(attacker.applyOnMissSkills(context, this, round));
            }
            return false;
        }
        int mitigated = 0;
        for (var entry : roll.attack().entrySet()) {
            mitigated += (int) (entry.getValue() * defenseReduce.get(entry.getKey()));
        }
        final int damageTaken = takeDamage(roll.amount(), mitigated);
        attacker.recordDamageDealt(damageTaken, roll.crit());
        log.add(new BattleEvent.DamageReceived(id, attacker.id(), roll, damageTaken, health, round));
        if (isAlive()) {
            log.addAll(applyOnDamageReceiveSkills(context, attacker, round));
            if (roll.crit() && attacker.isAlive()) {
                log.addAll(applyOnCritReceiveSkills(context, attacker, round));
            }
        }
        if (attacker.isAlive()) {
            log.addAll(attacker.applyOnHitSkills(context, this, round));
            if (roll.crit() && isAlive()) {
                log.addAll(attacker.applyOnCritSkills(context, this, round));
            }
        }
        return true;
    }

    /**
     * Skill-triggered damage: no dodge, mitigation + variance applied, emits {@link BattleEvent.SkillDamage}.
     * Death is NOT logged here — callers are responsible for detecting and logging {@link BattleEvent.PersonageDefeated}.
     */
    public List<BattleEvent> applySkillDamage(AttackType type, int rawAttack, UUID sourceId, ActiveEnum skill, int round) {
        if (!isAlive()) {
            return List.of();
        }
        final int damageTaken = takeDamage(rawAttack, (int) (rawAttack * defenseReduce.get(type)));
        return List.of(new BattleEvent.SkillDamage(id, sourceId, skill, type, damageTaken, health, round));
    }

    /**
     * Environmental / timed damage: no dodge, same mitigation variance as normal hits.
     */
    public void applyEffectDamage(AttackType type, int rawAttack, UUID sourceId, ActiveEnum skill, BattleActionLog log, int round) {
        if (!isAlive()) {
            return;
        }
        final int damageTaken = takeDamage(rawAttack, (int) (rawAttack * defenseReduce.get(type)));
        log.add(new BattleEvent.EffectDamage(id, sourceId, skill, type, damageTaken, health, round));
        if (!isAlive()) {
            log.add(new BattleEvent.PersonageDefeated(id, sourceId, round));
        }
    }

    /**
     * Core stat-tracking and health-reduction step shared by all damage paths.
     * {@code rawAmount} is the pre-mitigation value used only for blocked-damage stats;
     * {@code mitigated} is the post-mitigation value that variance and health subtraction operate on.
     *
     * @return actual damage taken (post-mitigation, post-variance)
     */
    private int takeDamage(int rawAmount, int mitigated) {
        bonusThreat = Math.max(0, bonusThreat - THREAT_LOSE_FROM_DAMAGE);
        damageBlocked += rawAmount;
        blockCount++;
        final int healthBefore = health;
        final int withVariance = RandomUtils.getInPercentRange(mitigated, RANGE_PERCENT);
        this.health = Math.max(0, health - withVariance);
        final int damageTaken = healthBefore - health;
        actualDamageTaken += damageTaken;
        return damageTaken;
    }

    public Set<AttackType> attackTypes() {
        return attackTypes;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void heal(int amount) {
        if (!isAlive() || amount <= 0) {
            return;
        }
        this.health = Math.min(maxHealth, health + amount);
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
    public boolean move(BattleContext context, BattleActionLog log, int round) {
        combatEffects.onOwnTurnBegin(this, log, round);
        if (!isAlive()) {
            return false;
        }
        log.addAll(applyTurnStartSkills(context, round));
        if (!isAlive()) {
            return false;
        }
        final var enemyAliveTeam = context.enemyAliveTeam(this);
        // поддержка списка сразу на будущее
        final var targets = randomAlivePersonage(this, enemyAliveTeam);
        if (targets.isEmpty()) {
            stepOneLineTowardEnemy();
            log.add(new BattleEvent.MovedTowardEnemy(id, currentPosition, round));
            return enemyAliveTeam.values().stream().noneMatch(BattlePersonage::isAlive);
        }
        final var target = targets.getFirst();
        final var personage = target.personage;
        final boolean hit = personage.receiveDamageFrom(this, rollDamage(target.range), log, round, context);
        if (!personage.isAlive()) {
            log.add(new BattleEvent.PersonageDefeated(personage.id(), id, round));
            bonusThreat += THREAT_FROM_KILL;
            enemyAliveTeam.remove(personage.id());
        } else if (hit) {
            bonusThreat += THREAT_FROM_DAMAGE;
        }
        if (!isAlive()) {
            log.add(new BattleEvent.PersonageDefeated(id, personage.id(), round));
            return false;
        }
        log.addAll(applyTurnEndSkills(context, round));
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

    private List<BattleEvent> applyTurnStartSkills(BattleContext context, int round) {
        return applySkills(turnStartSkills, context, round);
    }

    private List<BattleEvent> applyTurnEndSkills(BattleContext context, int round) {
        return applySkills(turnEndSkills, context, round);
    }

    private List<BattleEvent> applyOnHitSkills(BattleContext context, BattlePersonage target, int round) {
        return applySkills(onHitSkills, context, target, round);
    }

    private List<BattleEvent> applyOnCritSkills(BattleContext context, BattlePersonage target, int round) {
        return applySkills(onCritSkills, context, target, round);
    }

    private List<BattleEvent> applyOnMissSkills(BattleContext context, BattlePersonage target, int round) {
        return applySkills(onMissSkills, context, target, round);
    }

    private List<BattleEvent> applyOnDamageReceiveSkills(BattleContext context, BattlePersonage target, int round) {
        return applySkills(onDamageReceiveSkills, context, target, round);
    }

    private List<BattleEvent> applyOnCritReceiveSkills(BattleContext context, BattlePersonage target, int round) {
        return applySkills(onCritReceiveSkills, context, target, round);
    }

    private List<BattleEvent> applyOnDodgeSkills(BattleContext context, BattlePersonage target, int round) {
        return applySkills(onDodgeSkills, context, target, round);
    }

    private List<BattleEvent> applySkills(List<? extends TurnSkill> skills, BattleContext context, int round) {
        final var events = new ArrayList<BattleEvent>();
        for (final var skill : skills) {
            events.addAll(skill.apply(context, this, round));
        }
        return events;
    }

    private List<BattleEvent> applySkills(
        List<? extends DamageDealSkill> skills,
        BattleContext context,
        BattlePersonage target,
        int round
    ) {
        final var events = new ArrayList<BattleEvent>();
        for (final var skill : skills) {
            events.addAll(skill.apply(context, this, target, round));
        }
        return events;
    }

    public int percentHp() {
        return MathUtils.calcPercent(maxHealth, health);
    }

    public void increaseAttack(int percent) {
        for (int i = 1; i <= baseMaxRange; i++) {
            for (var entry : rangeAttack[i].entrySet()) {
                rangeAttack[i].put(entry.getKey(), MathUtils.addPercent(entry.getValue(), percent));
            }
            for (var entry : rangeAttack[i].entrySet()) {
                rangeAttackCrit[i].put(entry.getKey(), (int) (entry.getValue() * critMultiplier));
            }
        }
    }

    public void decreaseDefense(int percent) {
        for (var entry : defense.entrySet()) {
            entry.setValue(Math.max(0, MathUtils.removePercent(entry.getValue(), percent)));
        }
        refreshDefenseReduce();
    }

    private void refreshDefenseReduce() {
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
    }

    public Position startPosition() {
        return startPosition;
    }

    public int currentPosition() {
        return currentPosition;
    }

    public int range() {
        return baseMaxRange + combatEffects.maxRangeBonus();
    }

    public void increaseMaxRange(int delta, int ownMovesDuration) {
        combatEffects.addTemporaryMaxRange(new TemporaryMaxRangeBonus(delta, ownMovesDuration));
    }

    public void addPeriodicDamageEffect(PeriodicDamageEffect effect) {
        combatEffects.addPeriodicDamage(effect);
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

    public int maxHealth() {
        return maxHealth;
    }

    public int critChance() {
        return critChance;
    }

    public int dodgeChance() {
        return dodgeChance;
    }

    public double critMultiplier() {
        return critMultiplier;
    }

    public Map<DefenseType, Integer> defenses() {
        return Map.copyOf(defense);
    }

    public Map<AttackType, Integer> attackAtRange(int distance) {
        final int slot = Math.min(Math.max(1, distance), baseMaxRange);
        return Map.copyOf(rangeAttack[slot]);
    }

    public Map<AttackType, Double> defenseReductions() {
        return Map.copyOf(defenseReduce);
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

    public DamageRoll rollDamage(int strikeDistance) {
        final int clampedDistance = Math.min(Math.max(1, strikeDistance), range());
        final int mapSlot = Math.min(clampedDistance, baseMaxRange);
        if (RandomUtils.processChance(critChance)) {
            return new DamageRoll(rangeAttackCrit[mapSlot], true);
        }
        return new DamageRoll(rangeAttack[mapSlot], false);
    }

    public BattlePersonageStats battlePersonageStats() {
        return new BattlePersonageStats(
            maxHealth,
            health,
            totalAttackAtMinRange(),
            totalDefense(),
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

    private int totalAttackAtMinRange() {
        return rangeAttack[1].values().stream().mapToInt(Integer::intValue).sum();
    }

    private int totalDefense() {
        return defense.values().stream().mapToInt(Integer::intValue).sum();
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
        final var slotOneAttackSum = Math.max(
            1,
            rangeAttack[1].values().stream()
                .mapToInt(i -> i)
                .sum()
        );
        final var avgDamageTakenMultiplier = defenseReduce.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(1.0);
        final var critProbability = critChance / 100.0;
        final var effectiveDamage = Math.max(1, slotOneAttackSum * (1 + critProbability * (critMultiplier - 1)));
        final var dodgeProbability = dodgeChance / 100.0;
        final var hitChanceMultiplier = Math.max(1, 1 - dodgeProbability);

        final double expectedDamagePerTurn = effectiveDamage
            * avgDamageTakenMultiplier
            * hitChanceMultiplier;
        final var skillInputs = new SkillPowerInputs(
            dodgeChance,
            critChance,
            slotOneAttackSum,
            maxHealth,
            expectedDamagePerTurn
        );
        double offensiveDps = 0;
        double hpBonus = 0;
        for (var skill : itemSkills) {
            if (skill instanceof HealthPowerSkill) {
                hpBonus += skill.skillPowerRating(skillInputs);
            } else if (skill instanceof AttackPowerSkill) {
                offensiveDps += skill.skillPowerRating(skillInputs);
            }
        }

        final var healthFactor = Math.max(1, maxHealth + hpBonus);
        final var damageFactor = Math.max(1, effectiveDamage + offensiveDps);
        final var speedFactor = Math.max(1, speed);

        return healthFactor * damageFactor / avgDamageTakenMultiplier / hitChanceMultiplier
            * ((double) speedFactor / REQUIRED_SPEED);
    }

    private record Target(
        int range,
        BattlePersonage personage
    ) {
    }
}
