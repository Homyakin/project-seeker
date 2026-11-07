package ru.homyakin.seeker.game.battle;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import ru.homyakin.seeker.game.battle.effect.PeriodicDamageEffect;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.active_impl.CounterAttack;
import ru.homyakin.seeker.game.battle.skill.active_impl.Thorns;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;

class BattleTempoTest {
    private static final int RAW_ATTACK = 101;
    private static final int HEALTH = 10_000;
    private static final long SEED = 2_026_110_207L;
    private static final BattleRandom MIDPOINT = (sequence, minimum, maximum) -> {
        if (sequence.startsWith("skill-damage-")) {
            return 0;
        }
        if (sequence.startsWith("normal-damage:")) {
            return minimum + (maximum - minimum) / 2;
        }
        return minimum;
    };

    @ParameterizedTest
    @MethodSource("normalCases")
    void legacyOrdinaryAndCriticalHitsUseTheSelectedTempoOnce(BattleTempoVersion tempo, boolean critical) {
        final var source = personage(false, HEALTH, Map.of());
        final var target = personage(false, HEALTH, Map.of());
        final var context = context(source, target, tempo);
        final var log = new BattleActionLog();
        final int raw = critical ? 2 * RAW_ATTACK : RAW_ATTACK;
        final int unscaled = RandomUtils.withSeed(SEED, () -> {
            RandomUtils.processChance(0);
            return RandomUtils.getInPercentRange(raw, 10);
        });

        final boolean hit = RandomUtils.withSeed(SEED, () -> target.receiveDamageFrom(
            source, new DamageRoll(Map.of(AttackType.SLASH, raw), critical), log, 1, context
        ));

        Assertions.assertTrue(hit);
        assertSingleDamage(target, source, log.events(), HEALTH, referenceScale(unscaled, tempo), true);
        final var event = (BattleEvent.DamageReceived) log.events().getFirst();
        Assertions.assertEquals(raw, event.roll().amount(), "Raw attack basis must not be scaled");
        Assertions.assertEquals(critical, event.roll().crit());
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void legacyDirectSkillsAndOverkillKeepActualHealthAndCreditConsistent(BattleTempoVersion tempo) {
        final var source = personage(false, HEALTH, Map.of());
        final var target = personage(false, HEALTH, Map.of());
        context(source, target, tempo);
        final int unscaled = RandomUtils.withSeed(SEED, () -> RandomUtils.getInPercentRange(RAW_ATTACK, 10));

        final var events = RandomUtils.withSeed(SEED, () -> target.applySkillDamage(
            AttackType.SLASH, RAW_ATTACK, source, ActiveEnum.DOUBLE_ATTACK, 1
        ));

        assertSingleDamage(target, source, events, HEALTH, referenceScale(unscaled, tempo), true);
        final var victim = personage(false, 23, Map.of());
        final var killer = personage(false, HEALTH, Map.of());
        context(killer, victim, tempo);
        final var lethal = RandomUtils.withSeed(SEED, () -> victim.applySkillDamage(
            AttackType.SLASH, 1_000_000, killer, ActiveEnum.DOUBLE_ATTACK, 1
        ));
        assertSingleDamage(victim, killer, lethal, 23, 23, true);
        Assertions.assertTrue(victim.applySkillDamage(
            AttackType.SLASH, RAW_ATTACK, killer, ActiveEnum.THORNS, 2
        ).isEmpty());
        Assertions.assertEquals(0, victim.healAndGetActual(17));
        Assertions.assertEquals(0, victim.health());
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void legacyPeriodicDamageScalesEachTickOnceWithoutInventingSourceCredit(BattleTempoVersion tempo) {
        final var source = personage(false, HEALTH, Map.of());
        final var target = personage(false, HEALTH, Map.of());
        context(source, target, tempo);
        final var effect = new PeriodicDamageEffect(
            AttackType.SLASH, RAW_ATTACK, 1, 2, source.id(), ActiveEnum.BLEEDING
        );
        final int unscaled = RandomUtils.withSeed(SEED, () -> RandomUtils.getInPercentRange(RAW_ATTACK, 10));
        for (int tick = 1; tick <= 2; tick++) {
            final int round = tick;
            final int healthBefore = target.health();
            final var log = new BattleActionLog();

            final boolean exhausted = RandomUtils.withSeed(SEED, () -> effect.tickOnOwnTurnBegin(target, log, round));

            Assertions.assertEquals(tick == 2, exhausted);
            assertSingleDamage(target, source, log.events(), healthBefore, referenceScale(unscaled, tempo), false);
        }
    }

    @ParameterizedTest
    @MethodSource("normalCases")
    void legacyCounterAndThornsUseTheSameTempo(BattleTempoVersion tempo, boolean counter) {
        final var source = personage(false, HEALTH, Map.of());
        final var target = personage(false, HEALTH, Map.of());
        final var context = context(source, target, tempo);
        final long seed = counter ? counterSeed() : SEED;
        final int unscaled = RandomUtils.withSeed(seed, () -> {
            if (counter) {
                Assertions.assertTrue(RandomUtils.processChance(50));
                RandomUtils.getRandomElement(source.attackTypes());
            }
            return RandomUtils.getInPercentRange(counter ? 10 : 5, 10);
        });

        final var events = RandomUtils.withSeed(seed, () -> counter
            ? new CounterAttack(1).apply(context, source, target, 1)
            : new Thorns(1).apply(context, source, target, 1));

        assertSingleDamage(target, source, events, HEALTH, referenceScale(unscaled, tempo), true);
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void orderedScalingSkillsAndBothBleedingKindsKeepTheirBasesUnscaled(BattleTempoVersion tempo) {
        final var attacker = personage(true, HEALTH, Map.of(ActiveEnum.DOUBLE_ATTACK, 1, ActiveEnum.BLEEDING, 1));
        final var defender = personage(true, HEALTH, Map.of(ActiveEnum.COUNTER_ATTACK, 1, ActiveEnum.THORNS, 1));
        final var context = context(attacker, defender, tempo);
        final var log = new BattleActionLog();

        attacker.move(context, log, 1);
        Assertions.assertEquals(1, defender.scalingBleedings().size());
        defender.addPeriodicDamageEffect(new PeriodicDamageEffect(
            AttackType.SLASH, RAW_ATTACK, 1, 1, attacker.id(), ActiveEnum.BLEEDING
        ));
        final int unscaledLegacyTick = RandomUtils.withSeed(SEED, () -> RandomUtils.getInPercentRange(RAW_ATTACK, 10));
        RandomUtils.withSeed(SEED, () -> defender.move(context, log, 2));

        final var paths = assertLedger(List.of(attacker, defender), log.events(), tempo,
            Map.of("effect:BLEEDING", unscaledLegacyTick));
        Assertions.assertTrue(paths.containsAll(Set.of(
            "normal", "scaling:DOUBLE_ATTACK", "scaling:COUNTER_ATTACK", "scaling:THORNS",
            "scaling-periodic:BLEEDING", "effect:BLEEDING"
        )));
        Assertions.assertEquals(17, defender.healAndGetActual(17), "Healing is not tempo-scaled");
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void oldNpcAttacksAndReactionsScaleInTheNewCommonTurnOrder(BattleTempoVersion tempo) {
        final var player = personage(true, HEALTH, Map.of());
        final var npc = personage(false, HEALTH, Map.of(ActiveEnum.COUNTER_ATTACK, 1, ActiveEnum.THORNS, 1));
        final var context = context(player, npc, tempo);
        Assertions.assertTrue(context.usesScalingSkillOrder());
        Assertions.assertFalse(npc.usesScalingCombatRules());
        final long seed = counterSeed();
        final var unscaled = RandomUtils.withSeed(seed, () -> {
            Assertions.assertTrue(RandomUtils.processChance(50));
            RandomUtils.getRandomElement(npc.attackTypes());
            final int counter = RandomUtils.getInPercentRange(10, 10);
            final int thorns = RandomUtils.getInPercentRange(5, 10);
            return Map.of("legacy:COUNTER_ATTACK", counter, "legacy:THORNS", thorns);
        });
        final var log = new BattleActionLog();

        RandomUtils.withSeed(seed, () -> player.move(context, log, 1));
        npc.move(context, log, 2);

        final var paths = assertLedger(List.of(player, npc), log.events(), tempo, unscaled);
        Assertions.assertTrue(paths.containsAll(unscaled.keySet()));
        Assertions.assertEquals(2, log.events().stream().filter(BattleEvent.DamageReceived.class::isInstance).count());
        Assertions.assertEquals(tempo, npc.battleTempoVersion());
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void scalingOrdinaryOverkillClampsHealthAndLogsExactlyOneDefeat(BattleTempoVersion tempo) {
        final var source = personage(true, HEALTH, Map.of());
        final var target = personage(true, 23, Map.of());
        final var context = context(source, target, tempo);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        assertLedger(List.of(source, target), log.events(), tempo, Map.of());
        Assertions.assertEquals(0, target.health());
        Assertions.assertEquals(23, target.actualDamageTaken());
        Assertions.assertEquals(23, source.battlePersonageStats().damageDealt());
        Assertions.assertEquals(1, log.events().stream().filter(BattleEvent.PersonageDefeated.class::isInstance).count());
        Assertions.assertEquals(0, target.applyVersionedSkillDamage(RAW_ATTACK, RAW_ATTACK, source));
        Assertions.assertEquals(0, target.healAndGetActual(Integer.MAX_VALUE));
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void multiplierIsAppliedAfterMitigationAndTheSelectedVarianceRoll(BattleTempoVersion tempo) {
        final var source = personage(true, HEALTH, Map.of());
        final var armor = new ItemObject(
            "tempo_defense_test", Set.of(PersonageSlot.BODY), Optional.empty(),
            Optional.of(new ItemDefense(DefenseType.PLATE, 100)),
            HEALTH, 0, 0, 0.0, 0, 0, Map.of()
        );
        final var target = BattlePersonage.forScalingSkillsV2Combat(
            List.of(Item.fromObject(armor)), Position.FRONT, Map.of(), Optional.empty()
        );
        final BattleRandom minimumRoll = (sequence, minimum, maximum) -> minimum;
        final var context = new BattleContext(List.of(source), List.of(target), minimumRoll, tempo);
        final int mitigated = (int) (RAW_ATTACK * target.defenseReductions().get(AttackType.SLASH));
        final int selectedVariance = (int) Math.round(mitigated * 0.9);
        final var log = new BattleActionLog();

        source.move(context, log, 1);

        final var damage = log.events().stream().filter(BattleEvent.DamageReceived.class::isInstance)
            .map(BattleEvent.DamageReceived.class::cast).findFirst().orElseThrow();
        Assertions.assertEquals(RAW_ATTACK, damage.roll().amount());
        Assertions.assertEquals(referenceScale(selectedVariance, tempo), damage.damageTaken());
        Assertions.assertEquals(HEALTH - damage.damageTaken(), target.health());
    }

    @Test
    void targetingEstimateAndBothParticipantsFollowContextTempoAndResetToBaseline() {
        final var source = personage(true, HEALTH, Map.of());
        final var target = personage(false, HEALTH, Map.of());
        Assertions.assertEquals(RAW_ATTACK, source.expectedDamageAgainst(target));
        context(source, target, BattleTempoVersion.ACCELERATED_V1);

        Assertions.assertEquals(RAW_ATTACK * 1.75, source.expectedDamageAgainst(target));
        Assertions.assertEquals(RAW_ATTACK * 1.75, target.expectedDamageAgainst(source));

        new BattleContext(List.of(source), List.of(target), MIDPOINT);

        Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, source.battleTempoVersion());
        Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, target.battleTempoVersion());
        Assertions.assertEquals(RAW_ATTACK, source.expectedDamageAgainst(target));
        Assertions.assertEquals(RAW_ATTACK, target.expectedDamageAgainst(source));
    }

    @ParameterizedTest
    @EnumSource(BattleTempoVersion.class)
    void explicitBattleOverloadsCaptureSelectedTempoWithoutChangingRawSnapshotAttacks(BattleTempoVersion tempo) {
        for (final boolean bounded : List.of(false, true)) {
            final var source = personage(true, HEALTH, Map.of());
            final var target = personage(false, 1, Map.of());
            final var battle = new Battle(SEED);

            final var result = bounded
                ? battle.process(List.of(source), List.of(target), 100, tempo)
                : battle.process(List.of(source), List.of(target), tempo);

            Assertions.assertEquals(tempo, result.initState().tempoVersion());
            for (final var snapshot : result.initState().personagesById().values()) {
                Assertions.assertEquals(RAW_ATTACK, snapshot.attacksByRange().getFirst().attack().get(AttackType.SLASH));
            }
            Assertions.assertTrue(result.rounds() > 0 && result.rounds() <= 100);
        }
    }

    @Test
    void historicalBattleOverloadsDoNotSilentlyAccelerateV2Participants() {
        for (final boolean bounded : List.of(false, true)) {
            final var source = personage(true, HEALTH, Map.of());
            final var target = personage(false, 1, Map.of());
            source.setBattleTempoVersion(BattleTempoVersion.ACCELERATED_V1);
            target.setBattleTempoVersion(BattleTempoVersion.ACCELERATED_V1);
            final var battle = new Battle(SEED);

            final var result = bounded
                ? battle.process(List.of(source), List.of(target), 100)
                : battle.process(List.of(source), List.of(target));

            Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, result.initState().tempoVersion());
            Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, source.battleTempoVersion());
            Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, target.battleTempoVersion());
        }
    }

    private static Stream<Arguments> normalCases() {
        return Stream.of(BattleTempoVersion.values()).flatMap(tempo -> Stream.of(
            Arguments.of(tempo, false), Arguments.of(tempo, true)
        ));
    }

    private static BattleContext context(BattlePersonage source, BattlePersonage target, BattleTempoVersion tempo) {
        return new BattleContext(List.of(source), List.of(target), MIDPOINT, tempo);
    }

    private static BattlePersonage personage(boolean scaling, int health, Map<ActiveEnum, Integer> skills) {
        final var object = new ItemObject(
            "tempo_test", Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, RAW_ATTACK)), Optional.empty(),
            health, 0, 0, 0.0, 100, 0, Map.of()
        );
        final var items = List.of(new Item(object, Optional.empty(), ItemRarity.COMMON));
        return scaling
            ? BattlePersonage.forScalingSkillsV2Combat(items, Position.FRONT, skills, Optional.empty())
            : new BattlePersonage(items, Position.FRONT, skills);
    }

    private static int referenceScale(int damage, BattleTempoVersion tempo) {
        return tempo == BattleTempoVersion.BASELINE_V1 ? damage : Math.toIntExact(((long) damage * 7 + 2) / 4);
    }

    private static long counterSeed() {
        for (long candidate = 0; candidate < 1_000; candidate++) {
            final long seed = candidate;
            if (RandomUtils.withSeed(seed, () -> RandomUtils.processChance(50))) {
                return seed;
            }
        }
        throw new AssertionError("No deterministic successful counter seed");
    }

    private static void assertSingleDamage(
        BattlePersonage target,
        BattlePersonage source,
        List<BattleEvent> events,
        int healthBefore,
        int expected,
        boolean credited
    ) {
        Assertions.assertEquals(1, events.size());
        final var event = events.getFirst();
        final int actual;
        final int remaining;
        if (event instanceof BattleEvent.DamageReceived damage) {
            actual = damage.damageTaken();
            remaining = damage.remainingHealth();
        } else if (event instanceof BattleEvent.SkillDamage damage) {
            actual = damage.damageTaken();
            remaining = damage.remainingHealth();
        } else if (event instanceof BattleEvent.EffectDamage damage) {
            actual = damage.damageTaken();
            remaining = damage.remainingHealth();
        } else {
            throw new AssertionError("Unexpected event " + event);
        }
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(healthBefore - expected, target.health());
        Assertions.assertEquals(target.health(), remaining);
        Assertions.assertEquals(target.maxHealth() - target.health(), target.actualDamageTaken());
        Assertions.assertEquals(credited ? expected : 0, source.battlePersonageStats().damageDealt());
    }

    private static Set<String> assertLedger(
        List<BattlePersonage> members,
        List<BattleEvent> events,
        BattleTempoVersion tempo,
        Map<String, Integer> legacyUnscaled
    ) {
        final var health = new HashMap<UUID, Integer>();
        final var credited = new HashMap<UUID, Long>();
        final var paths = new HashSet<String>();
        for (final var member : members) {
            health.put(member.id(), member.maxHealth());
            credited.put(member.id(), 0L);
        }
        for (final var event : events) {
            final UUID target;
            final UUID source;
            final int actual;
            final int remaining;
            final int unscaled;
            boolean givesCredit = true;
            if (event instanceof BattleEvent.DamageReceived damage) {
                target = damage.targetId();
                source = damage.attackerId();
                actual = damage.damageTaken();
                remaining = damage.remainingHealth();
                Assertions.assertFalse(damage.roll().crit());
                Assertions.assertEquals(RAW_ATTACK, damage.roll().amount());
                unscaled = RAW_ATTACK;
                paths.add("normal");
            } else if (event instanceof BattleEvent.ScalingSkillDamage damage) {
                target = damage.targetId();
                source = damage.sourceId();
                actual = damage.damageTaken();
                remaining = damage.remainingHealth();
                final int basis = damage.basis().values().stream().mapToInt(Integer::intValue).sum();
                Assertions.assertEquals(damage.skill() == ActiveEnum.THORNS ? 40 * RAW_ATTACK : RAW_ATTACK, basis);
                unscaled = Math.max(1, BigInteger.valueOf(basis)
                    .multiply(BigInteger.valueOf(damage.coefficientNumerator()))
                    .divide(BigInteger.valueOf(damage.coefficientDenominator())).intValueExact());
                paths.add((damage.periodic() ? "scaling-periodic:" : "scaling:") + damage.skill());
            } else if (event instanceof BattleEvent.SkillDamage damage) {
                target = damage.targetId();
                source = damage.sourceId();
                actual = damage.damageTaken();
                remaining = damage.remainingHealth();
                unscaled = legacyUnscaled.get("legacy:" + damage.skill());
                paths.add("legacy:" + damage.skill());
            } else if (event instanceof BattleEvent.EffectDamage damage) {
                target = damage.targetId();
                source = damage.sourceId();
                actual = damage.damageTaken();
                remaining = damage.remainingHealth();
                unscaled = legacyUnscaled.get("effect:" + damage.skill());
                givesCredit = false;
                paths.add("effect:" + damage.skill());
            } else {
                continue;
            }
            final int before = health.get(target);
            final int expected = Math.min(before, referenceScale(unscaled, tempo));
            Assertions.assertEquals(expected, actual, () -> "Exactly one tempo multiplier: " + event);
            Assertions.assertEquals(before - actual, remaining);
            health.put(target, remaining);
            if (givesCredit) {
                credited.put(source, credited.get(source) + actual);
            }
        }
        for (final var member : members) {
            Assertions.assertEquals(health.get(member.id()).intValue(), member.health());
            Assertions.assertEquals(member.maxHealth() - member.health(), member.actualDamageTaken());
            Assertions.assertEquals(credited.get(member.id()).longValue(), member.battlePersonageStats().damageDealt());
        }
        return Set.copyOf(paths);
    }
}
