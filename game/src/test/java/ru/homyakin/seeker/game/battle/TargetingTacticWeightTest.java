package ru.homyakin.seeker.game.battle;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingMath;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.battle.targeting.TargetingTacticCoefficients;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.utils.RandomUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetingTacticWeightTest {

    @Test
    void threatTacticKeepsThreatWeights() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        final var tank = place(personage(Item.stats(0, 0, 1.2, 50, 60), highHpArmor()));
        final var dps = place(personage(Item.stats(0, 0, 1.2, 50, 20), highHpArmor()));
        attacker.setTargetingTactic(TargetingTactic.THREAT);

        final var weights = attacker.targetingWeights(List.of(tank, dps));

        assertEquals(60, weights.get(tank));
        assertEquals(20, weights.get(dps));
    }

    @Test
    void woundedHunterPrefersLowerPercentHp() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.WOUNDED_HUNTER);

        final var healthy = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.CLOTH, 0, 1000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));
        final var wounded = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.CLOTH, 0, 1000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));
        damageToPercent(wounded, 20);

        final var beforeThreat = wounded.totalThreat();
        final var weights = attacker.targetingWeights(List.of(healthy, wounded));

        assertTrue(weights.get(wounded) > weights.get(healthy));
        assertEquals(beforeThreat, wounded.totalThreat());
    }

    @Test
    void executionerBoostsNearKillTarget() {
        final var attacker = place(personage(
            Item.stats(0, 0, 1.2, 100, 10),
            Item.weapon(AttackType.SLASH, 1, 200, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
        ));
        attacker.setTargetingTactic(TargetingTactic.EXECUTIONER);

        final var full = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.CLOTH, 0, 5000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));
        final var low = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.CLOTH, 0, 5000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));
        try (final var random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.getInPercentRange(Mockito.anyInt(), Mockito.anyDouble()))
                .thenAnswer(invocation -> invocation.getArgument(0));
            low.applyEffectDamage(
                AttackType.SLASH,
                low.health() - 50,
                attacker.id(),
                ActiveEnum.BLEEDING,
                new BattleActionLog(),
                1
            );
        }

        final var weights = attacker.targetingWeights(List.of(full, low));
        assertTrue(weights.get(low) > weights.get(full));
    }

    @Test
    void reliableStrikePrefersLowDodge() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.RELIABLE_STRIKE);

        final var lowDodge = place(personage(Item.stats(0, 0, 1.2, 50, 20), highHpArmor()));
        final var highDodge = place(personage(Item.stats(0, 50, 1.2, 50, 20), highHpArmor()));

        final var weights = attacker.targetingWeights(List.of(lowDodge, highDodge));
        assertTrue(weights.get(lowDodge) > weights.get(highDodge));
    }

    @Test
    void challengeTheAgilePrefersHighDodge() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.CHALLENGE_THE_AGILE);

        final var lowDodge = place(personage(Item.stats(0, 0, 1.2, 50, 20), highHpArmor()));
        final var highDodge = place(personage(Item.stats(0, 50, 1.2, 50, 20), highHpArmor()));

        final var weights = attacker.targetingWeights(List.of(lowDodge, highDodge));
        assertTrue(weights.get(highDodge) > weights.get(lowDodge));
    }

    @Test
    void initiativeInterceptionUsesAbsoluteWindow() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.INITIATIVE_INTERCEPTION);

        final var soon = place(personage(Item.stats(0, 0, 1.2, 100, 20), highHpArmor()));
        final var far = place(personage(Item.stats(0, 0, 1.2, 100, 20), highHpArmor()));
        final var alsoFar = place(personage(Item.stats(0, 0, 1.2, 100, 20), highHpArmor()));
        soon.setInitiativeGaugeForTest(950);
        far.setInitiativeGaugeForTest(0);
        alsoFar.setInitiativeGaugeForTest(50);

        final var weights = attacker.targetingWeights(List.of(soon, far, alsoFar));
        assertTrue(weights.get(soon) > weights.get(far));
        assertEquals(weights.get(far), weights.get(alsoFar));
    }

    @Test
    void initiativeInterceptionPrefersReadyToActOverHighGauge() {
        // Real round flow: tick → movers queue → attacker acts before Enemy A.
        final var attacker = personage(
            Item.stats(0, 0, 1.2, 100, 10),
            Item.weapon(AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON),
            highHpArmor()
        );
        attacker.setTargetingTactic(TargetingTactic.INITIATIVE_INTERCEPTION);

        final var enemyA = personage(Item.stats(0, 0, 1.2, 100, 20), highHpArmor());
        final var enemyB = personage(Item.stats(0, 0, 1.2, 100, 20), highHpArmor());
        // After tick: A wraps past threshold (low leftover gauge), B stays just short with higher gauge.
        enemyA.setInitiativeGaugeForTest(950);
        enemyB.setInitiativeGaugeForTest(800);
        attacker.setInitiativeGaugeForTest(950);

        final var context = new BattleContext(List.of(attacker), List.of(enemyA, enemyB));
        final var log = new BattleActionLog();

        assertTrue(attacker.tick(log, 1));
        assertTrue(enemyA.tick(log, 1));
        assertFalse(enemyB.tick(log, 1));

        assertTrue(attacker.readyToAct());
        assertTrue(enemyA.readyToAct());
        assertFalse(enemyB.readyToAct());
        assertTrue(enemyA.initiativeGauge() < enemyB.initiativeGauge());
        assertTrue(enemyA.ticksUntilNextTurn() > enemyB.ticksUntilNextTurn());

        // Attacker moves first while A is still queued in movers with readyToAct.
        final var weightsBeforeAttackerMove = attacker.targetingWeights(List.of(enemyA, enemyB));
        assertTrue(weightsBeforeAttackerMove.get(enemyA) > weightsBeforeAttackerMove.get(enemyB));

        attacker.move(context, log, 1);
        assertFalse(attacker.readyToAct());
        assertTrue(enemyA.readyToAct());

        final var weightsAfterAttackerMove = attacker.targetingWeights(List.of(enemyA, enemyB));
        assertTrue(weightsAfterAttackerMove.get(enemyA) > weightsAfterAttackerMove.get(enemyB));

        enemyA.move(context, log, 1);
        assertFalse(enemyA.readyToAct());
    }

    @Test
    void expectedDamageClampsCritChanceAbove100() {
        final var target = place(personage(highHpArmor()));
        final var crit100 = place(personage(
            Item.stats(100, 0, 1.5, 100, 10),
            Item.weapon(AttackType.SLASH, 1, 100, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
        ));
        final var crit150 = place(personage(
            Item.stats(150, 0, 1.5, 100, 10),
            Item.weapon(AttackType.SLASH, 1, 100, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
        ));

        assertEquals(crit100.expectedDamageAgainst(target), crit150.expectedDamageAgainst(target), 1e-9);
    }

    @Test
    void expectedDamageClampsHitProbabilityToUnitInterval() {
        final var attacker = place(personage(
            Item.stats(0, 0, 1.2, 100, 10),
            Item.weapon(AttackType.SLASH, 1, 100, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
        ));
        final var noDodge = place(personage(
            Item.stats(0, 0, 1.2, 50, 20),
            highHpArmor()
        ));
        final var overDodge = place(personage(
            Item.stats(0, 150, 1.2, 50, 20),
            highHpArmor()
        ));
        final var negativeDodge = place(personage(
            Item.stats(0, -50, 1.2, 50, 20),
            highHpArmor()
        ));

        final var fullHitEv = attacker.expectedDamageAgainst(noDodge);
        assertEquals(0.0, attacker.expectedDamageAgainst(overDodge), 1e-9);
        assertEquals(fullHitEv, attacker.expectedDamageAgainst(negativeDodge), 1e-9);
        assertTrue(fullHitEv > 0.0);
    }

    @Test
    void exploitWeaknessEqualEvDoesNotChangeWeights() {
        final var attacker = place(personage(
            Item.stats(0, 0, 1.2, 100, 10),
            Item.weapon(AttackType.SLASH, 1, 100, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
        ));
        attacker.setTargetingTactic(TargetingTactic.EXPLOIT_WEAKNESS);

        final var a = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.CLOTH, 100, 5000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));
        final var b = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.CLOTH, 100, 5000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));

        final var weights = attacker.targetingWeights(List.of(a, b));
        assertEquals(20, weights.get(a));
        assertEquals(20, weights.get(b));
    }

    @Test
    void exploitWeaknessTinyEvGapGivesTinyBonus() {
        assertEquals(0.01, TargetingMath.relativeAdvantageScore(100, 99, 100), 1e-9);
        assertEquals(0.0, TargetingMath.relativeAdvantageScore(99, 99, 100), 1e-9);

        final var betterWeight = TargetingMath.targetingWeight(
            20, 0.01, 60, TargetingTacticCoefficients.EXPLOIT_BONUS_FACTOR
        );
        final var worseWeight = TargetingMath.targetingWeight(
            20, 0.0, 60, TargetingTacticCoefficients.EXPLOIT_BONUS_FACTOR
        );
        assertEquals(20, worseWeight);
        assertTrue(betterWeight > worseWeight);
        assertTrue(betterWeight - worseWeight <= 2);
    }

    @Test
    void exploitWeaknessPrefersHigherEvWhenGapIsMeaningful() {
        final var attacker = place(personage(
            Item.stats(0, 0, 1.2, 100, 10),
            Item.weapon(AttackType.SLASH, 1, 100, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON)
        ));
        attacker.setTargetingTactic(TargetingTactic.EXPLOIT_WEAKNESS);

        final var soft = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.CLOTH, 0, 5000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));
        final var hard = place(personage(Item.stats(0, 0, 1.2, 50, 20), Item.armor(
            DefenseType.PLATE, 500, 5000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON
        )));

        final var weights = attacker.targetingWeights(List.of(soft, hard));
        assertTrue(weights.get(soft) > weights.get(hard));
    }

    @Test
    void sameScorePreservesThreatDifference() {
        final var tankWeight = TargetingMath.targetingWeight(60, 1.0, 60, 1.5);
        final var targetWeight = TargetingMath.targetingWeight(20, 1.0, 60, 1.5);
        assertEquals(150, tankWeight);
        assertEquals(110, targetWeight);
        assertEquals(40, tankWeight - targetWeight);
    }

    @Test
    void idealMatchAddsBonusWithoutErasingThreat() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.RELIABLE_STRIKE);

        final var tank = place(personage(Item.stats(0, 100, 1.2, 50, 60), highHpArmor()));
        final var priority = place(personage(Item.stats(0, 0, 1.2, 50, 20), highHpArmor()));
        final var tankThreatBefore = tank.totalThreat();
        final var priorityThreatBefore = priority.totalThreat();

        final var weights = attacker.targetingWeights(List.of(tank, priority));
        // score=1, referenceThreat=60, BONUS_FACTOR=1.5 → 20 + 60 * 1.5 = 110
        assertEquals(110, weights.get(priority));
        assertEquals(60, weights.get(tank));
        assertEquals(tankThreatBefore, tank.totalThreat());
        assertEquals(priorityThreatBefore, priority.totalThreat());
        assertTrue(weights.get(priority) > weights.get(tank));
    }

    @Test
    void initiativeIdealMatchAddsDoubleReferenceBonus() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.INITIATIVE_INTERCEPTION);

        final var tank = place(personage(Item.stats(0, 0, 1.2, 50, 60), highHpArmor()));
        final var agile = place(personage(Item.stats(0, 0, 1.2, 50, 20), highHpArmor()));
        agile.setInitiativeGaugeForTest(999);
        tank.setInitiativeGaugeForTest(0);

        final var weights = attacker.targetingWeights(List.of(tank, agile));
        // score=1, referenceThreat=60, BONUS_FACTOR=2.0 → 20 + 120 = 140
        assertEquals(140, weights.get(agile));
        assertEquals(60, weights.get(tank));
    }

    @Test
    void weightFormulaAddsBonusProportionalToScore() {
        // base=20, ref=60, factor=1.5 → bonus at score=1 is 90
        assertEquals(20, TargetingMath.targetingWeight(20, 0.00, 60, 1.5));
        assertEquals(43, TargetingMath.targetingWeight(20, 0.25, 60, 1.5));
        assertEquals(65, TargetingMath.targetingWeight(20, 0.50, 60, 1.5));
        assertEquals(88, TargetingMath.targetingWeight(20, 0.75, 60, 1.5));
        assertEquals(110, TargetingMath.targetingWeight(20, 1.00, 60, 1.5));
    }

    @Test
    void weightFormulaNeverDropsBelowBaseThreat() {
        assertEquals(120, TargetingMath.targetingWeight(120, 0.0, 60, 1.75));
        assertTrue(TargetingMath.targetingWeight(120, 1.0, 60, 1.75) >= 120);
    }

    @Test
    void relativeAdvantageScoreHandlesEdgeCases() {
        assertEquals(0.0, TargetingMath.relativeAdvantageScore(50, 50, 50));
        assertEquals(0.0, TargetingMath.relativeAdvantageScore(10, 0, 0));
        assertEquals(0.5, TargetingMath.relativeAdvantageScore(100, 50, 100));
        assertEquals(1.0, TargetingMath.relativeAdvantageScore(100, 0, 100));
    }

    private static BattlePersonage personage(Item... items) {
        return new BattlePersonage(List.of(items), Position.FRONT);
    }

    private static Item highHpArmor() {
        return Item.armor(DefenseType.CLOTH, 0, 5000, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON);
    }

    private static BattlePersonage place(BattlePersonage personage) {
        personage.placeOnBattlefield(0, BattleAdvanceDirection.TOWARD_SECOND_TEAM);
        return personage;
    }

    private static void damageToPercent(BattlePersonage target, int percentHp) {
        final var desired = Math.max(1, target.maxHealth() * percentHp / 100);
        final var raw = target.health() - desired;
        try (final var random = Mockito.mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.getInPercentRange(Mockito.anyInt(), Mockito.anyDouble()))
                .thenAnswer(invocation -> invocation.getArgument(0));
            target.applyEffectDamage(
                AttackType.SLASH,
                raw,
                target.id(),
                ActiveEnum.BLEEDING,
                new BattleActionLog(),
                1
            );
        }
    }
}
