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
    void exploitWeaknessDoesNotAmplifyTinyEvGap() {
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
    void idealMatchOutprioritizesTankByPriorityFactor() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.RELIABLE_STRIKE);

        final var tank = place(personage(Item.stats(0, 100, 1.2, 50, 60), highHpArmor()));
        final var priority = place(personage(Item.stats(0, 0, 1.2, 50, 20), highHpArmor()));

        final var weights = attacker.targetingWeights(List.of(tank, priority));
        // score=1, referenceThreat=60, factor=1.5 → targetPriorityWeight=90
        assertEquals(
            TargetingMath.targetingWeight(
                20, 1.0, 60, TargetingTacticCoefficients.RELIABLE_PRIORITY_FACTOR
            ),
            weights.get(priority)
        );
        assertEquals(90, weights.get(priority));
        assertTrue(weights.get(priority) > weights.get(tank));
    }

    @Test
    void initiativeIdealMatchReachesDoubleReferenceThreat() {
        final var attacker = place(personage(Item.stats(0, 0, 1.2, 100, 10), Item.weapon(
            AttackType.SLASH, 1, 50, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON
        )));
        attacker.setTargetingTactic(TargetingTactic.INITIATIVE_INTERCEPTION);

        final var tank = place(personage(Item.stats(0, 0, 1.2, 50, 60), highHpArmor()));
        final var agile = place(personage(Item.stats(0, 0, 1.2, 50, 20), highHpArmor()));
        agile.setInitiativeGaugeForTest(999);
        tank.setInitiativeGaugeForTest(0);

        final var weights = attacker.targetingWeights(List.of(tank, agile));
        assertEquals(
            TargetingMath.targetingWeight(
                20, 1.0, 60, TargetingTacticCoefficients.INITIATIVE_PRIORITY_FACTOR
            ),
            weights.get(agile)
        );
        assertEquals(120, weights.get(agile));
        assertEquals(60, weights.get(tank));
    }

    @Test
    void weightFormulaInterpolatesAgainstReferenceThreat() {
        // Example from balance plan: base=20, ref=60, factor=1.75
        assertEquals(20, TargetingMath.targetingWeight(20, 0.00, 60, 1.75));
        assertEquals(41, TargetingMath.targetingWeight(20, 0.25, 60, 1.75));
        assertEquals(63, TargetingMath.targetingWeight(20, 0.50, 60, 1.75));
        assertEquals(84, TargetingMath.targetingWeight(20, 0.75, 60, 1.75));
        assertEquals(105, TargetingMath.targetingWeight(20, 1.00, 60, 1.75));
    }

    @Test
    void weightFormulaDoesNotReduceAboveTargetPriority() {
        assertEquals(120, TargetingMath.targetingWeight(120, 1.0, 60, 1.75));
        assertEquals(120, TargetingMath.targetingWeight(120, 0.0, 60, 1.75));
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
