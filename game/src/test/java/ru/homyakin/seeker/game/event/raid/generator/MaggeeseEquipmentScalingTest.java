package ru.homyakin.seeker.game.event.raid.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleSkillInitSnapshot;
import ru.homyakin.seeker.game.battle.BattleTempoVersion;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

/** Exact step 18 wiring and compatibility, not a target victory rate. */
class MaggeeseEquipmentScalingTest {
    private static final SkillFormulaVersion V3 = SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final long SEED = 20_261_115_180L;
    private static final double BONUS = 2.04;
    private static final double CORRECTION = 0.725;

    @ParameterizedTest
    @MethodSource("sizes")
    void supportedAndLargerSizesUseTheExactEquipmentSumAndFinalFactory(int size) {
        final var players = players(size);
        final var generated = generate(players, BONUS, V3);
        final int count = Math.max(2, size);

        Assertions.assertEquals(sum(players) * BONUS * group(count) * CORRECTION,
            generated.calibration().targetPower());
        Assertions.assertEquals(sum(generated.enemies()), generated.calibration().achievedPower());
        Assertions.assertEquals(count, generated.enemies().size());
        assertEnemies(generated, V3);
        final var diagnostics = generated.diagnostics().orElseThrow();
        Assertions.assertEquals("EQUIPMENT_POWER_SUM_V1", diagnostics.assessmentVersion());
        Assertions.assertEquals(sum(players), diagnostics.participantAssessment());
        Assertions.assertEquals(Map.of(
            "enemyCount", (double) count, "frontCount", (double) (count / 2),
            "midCount", (double) (count - count / 2), "groupSizeScaling", group(count),
            "powerBonus", BONUS, "encounterCorrection", CORRECTION), diagnostics.parameters());
    }

    @Test
    void correctionHasNoDownwardJumpAtTenElevenAndUsesFloatingPointDivision() {
        double previous = 0;
        for (int count = 2; count <= 10000; count++) {
            Assertions.assertTrue(group(count) >= previous);
            previous = group(count);
        }
        for (final int size : new int[]{1, 2, 9, 10, 11, 12, 30}) {
            final var players = players(size);
            final var generated = generate(players, 1, V3);
            Assertions.assertEquals(sum(players) * group(Math.max(2, size)) * CORRECTION,
                generated.calibration().targetPower());
        }
        Assertions.assertTrue(group(11) > group(10));
        Assertions.assertEquals(1 + MathUtils.log(2.2, 10) * 0.05, group(10));
        Assertions.assertTrue(group(11) > 1 + MathUtils.log(2.2, 10) * 0.05);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actualV3BattleStartsTheMagicalHalfInTheMiddle(boolean reverse) {
        final var players = players(3);
        final var enemies = generate(players, 1, V3).enemies();
        final var result = RandomUtils.withSeed(SEED, () -> new Battle(SEED).process(
            reverse ? players : enemies, reverse ? enemies : players, 2000, BattleTempoVersion.ACCELERATED_V1));
        for (int index = 0; index < enemies.size(); index++) {
            final var initial = result.initState().personagesById().get(enemies.get(index).id());
            Assertions.assertEquals(index < enemies.size() / 2 ? Position.FRONT : Position.MID,
                result.initState().lines().get(initial.lineIndex()).position());
        }
    }

    @Test
    void temporaryEffectsTacticAndPositionDoNotChangeThePermanentEquipmentTarget() {
        final var plain = players(1).getFirst();
        final var effects = new PersonageEffects(Map.of(PersonageEffectType.MENU_ITEM_EFFECT,
            new PersonageEffect(new Effect.Multiplier(20, EffectCharacteristic.ATTACK), null)));
        final var changed = BattlePersonage.forCombat(playerItems(), Position.BACK, effects, Optional.empty(), V3);
        changed.setTargetingTactic(TargetingTactic.WOUNDED_HUNTER);
        Assertions.assertNotEquals(plain.battlePersonageStats().attack(), changed.battlePersonageStats().attack());
        Assertions.assertEquals(plain.equipmentPowerUnscaled(), changed.equipmentPowerUnscaled());
        Assertions.assertEquals(generate(List.of(plain), BONUS, V3).calibration(),
            generate(List.of(changed), BONUS, V3).calibration());
    }

    @Test
    void permutationsAcrossDifferentMagnitudesHaveBitExactResultsAndFinalRandomness() {
        final var players = List.of(powerOnly(1e16), powerOnly(1), powerOnly(1));
        final var first = observe(players, 1, V3, false);
        final var reversed = observe(players.reversed(), 1, V3, false);
        Assertions.assertEquals(10_000_000_000_000_002.0,
            first.generated().diagnostics().orElseThrow().participantAssessment());
        assertSameGeneration(first, reversed);
    }

    @Test
    void trialEvaluationsDoNotConsumeFinalIdsInitiativeOrTheOuterRandomStream() {
        final var players = players(3);
        final var minimum = observe(players, 0, V3, false);
        final var normal = observe(players, BONUS, V3, false);
        Assertions.assertNotEquals(minimum.generated().calibration().evaluations(),
            normal.generated().calibration().evaluations());
        Assertions.assertEquals(minimum.generated().enemies().stream().map(BattlePersonage::id).toList(),
            normal.generated().enemies().stream().map(BattlePersonage::id).toList());
        Assertions.assertEquals(minimum.generated().enemies().stream().map(BattlePersonage::initiativeGauge).toList(),
            normal.generated().enemies().stream().map(BattlePersonage::initiativeGauge).toList());
        Assertions.assertEquals(minimum.nextDraw(), normal.nextDraw());
    }

    @Test
    void zeroBonusOrZeroPersonalPowerProducesTheExplicitNonzeroMinimum() {
        for (final var generated : List.of(generate(players(3), 0, V3), generate(List.of(powerOnly(0)), 1, V3))) {
            Assertions.assertEquals(0, generated.calibration().targetPower());
            Assertions.assertEquals(0, generated.calibration().multiplier());
            Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET,
                generated.calibration().status());
            Assertions.assertTrue(generated.enemies().stream().allMatch(enemy -> enemy.maxHealth() == 1));
            Assertions.assertTrue(generated.calibration().achievedPower() > 0);
            Assertions.assertEquals(sum(generated.enemies()), generated.calibration().achievedPower());
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.25, 0.75})
    void theNearestReachableIntegerStepIsSelected(double fraction) {
        final var players = players(1);
        final double lower = sum(List.of(expectedUnit(true, 0, V3), expectedUnit(false, 0, V3)));
        final double m = Math.nextUp(1.0 / 320);
        final double upper = sum(List.of(expectedUnit(true, m, V3), expectedUnit(false, m, V3)));
        Assertions.assertTrue(upper > lower);
        final double target = lower + fraction * (upper - lower);
        final var generated = generate(players, target / (sum(players) * group(2) * CORRECTION), V3);
        Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, generated.calibration().status());
        Assertions.assertEquals(fraction < 0.5 ? lower : upper, generated.calibration().achievedPower());
    }

    @Test
    void safeUpperBoundaryAndUnreachableTargetsAreExplicit() {
        final var players = players(2);
        final double m = (PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC - 1000.0) / 320;
        final double target = sum(List.of(expectedUnit(true, m, V3), expectedUnit(false, m, V3)));
        final var generated = generate(players, target / (sum(players) * group(2) * CORRECTION), V3);
        Assertions.assertTrue(generated.enemies().getFirst().maxHealth()
            > PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC * 0.99);
        Assertions.assertEquals(target, generated.calibration().achievedPower(), target * 1e-12);
        Assertions.assertEquals(sum(generated.enemies()), generated.calibration().achievedPower());
        final var failure = Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(players, 1e200 / (sum(players) * group(2) * CORRECTION), V3));
        Assertions.assertTrue(failure.getMessage().contains("unreachable"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void invalidPersonalPowerOrBonusIsRejectedWithoutLegacyFallback(double invalid) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(List.of(powerOnly(invalid)), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), invalid, V3));
    }

    @Test
    void nullEmptyOverflowAndMissingVersionInputsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(null, 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(List.of(), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(Arrays.asList((BattlePersonage) null), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), 1, null));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(List.of(powerOnly(Double.MAX_VALUE), powerOnly(Double.MAX_VALUE)), 0, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), Double.MAX_VALUE, V3));
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {
        "LEGACY_SKILLS_V1", "SCALING_SKILLS_V1", "SCALING_SKILLS_V2"
    })
    void historicalBodiesRetainExactCharacteristicsCalibrationIdsInitiativeAndRandomDraws(SkillFormulaVersion version) {
        for (final int size : new int[]{1, 2, 3, 9, 10, 11, 30}) {
            final var players = players(size);
            final var actual = observe(players, BONUS, version, false);
            final var historical = observe(players, BONUS, version, true);
            assertSameGeneration(historical, actual);
            Assertions.assertTrue(actual.generated().diagnostics().isEmpty());
            assertEnemies(actual.generated(), version);
        }
    }

    private static void assertEnemies(GeneratedPveEnemies generated, SkillFormulaVersion version) {
        for (int index = 0; index < generated.enemies().size(); index++) {
            final boolean front = index < generated.enemies().size() / 2;
            final var actual = generated.enemies().get(index);
            final var expected = expectedUnit(front, generated.calibration().multiplier(), version);
            Assertions.assertEquals(expected.battlePersonageStats(), actual.battlePersonageStats());
            Assertions.assertEquals(expected.attacksByRange(), actual.attacksByRange());
            Assertions.assertEquals(expected.defenses(), actual.defenses());
            Assertions.assertEquals(front ? Position.FRONT : Position.MID, actual.startPosition());
            Assertions.assertEquals(front || version != V3 ? 1 : 2, actual.range());
            Assertions.assertEquals(Set.of(front ? AttackType.SLASH : AttackType.MAGICAL), actual.attackTypes());
            Assertions.assertEquals(List.of(new BattleSkillInitSnapshot(
                front ? ActiveEnum.DOUBLE_ATTACK : ActiveEnum.HIT_AND_RUN, 4, version)), actual.skillSnapshots());
        }
    }

    private static void assertSameGeneration(Observed expected, Observed actual) {
        Assertions.assertEquals(expected.generated().calibration(), actual.generated().calibration());
        Assertions.assertEquals(expected.generated().diagnostics(), actual.generated().diagnostics());
        Assertions.assertEquals(expected.nextDraw(), actual.nextDraw());
        Assertions.assertEquals(expected.generated().enemies().size(), actual.generated().enemies().size());
        for (int index = 0; index < expected.generated().enemies().size(); index++) {
            final var first = expected.generated().enemies().get(index);
            final var second = actual.generated().enemies().get(index);
            Assertions.assertEquals(first.id(), second.id());
            Assertions.assertEquals(first.initiativeGauge(), second.initiativeGauge());
            Assertions.assertEquals(first.battlePersonageStats(), second.battlePersonageStats());
            Assertions.assertEquals(first.skillSnapshots(), second.skillSnapshots());
        }
    }

    /** Pre-step-18 order: each trial constructs one front and one mage, then multiplies their old powers. */
    private static GeneratedPveEnemies historical(List<BattlePersonage> players, double bonus, SkillFormulaVersion version) {
        final int count = Math.max(2, players.size());
        final double group = 1 + MathUtils.log(count <= 10 ? 2.2 : 4, count) * 0.05;
        final double target = players.stream().mapToDouble(BattlePersonage::legacyPower).sum() * bonus * group * 0.75;
        final var calibration = PveCharacteristicScaling.search(target,
            PveCharacteristicScaling.maxMultiplier(95, 70, 320, 120, 50, 270),
            m -> expectedUnit(true, m, version).legacyPower() * (count / 2)
                + expectedUnit(false, m, version).legacyPower() * (count - count / 2));
        final var enemies = new ArrayList<BattlePersonage>();
        for (int index = 0; index < count; index++) {
            enemies.add(expectedUnit(index < count / 2, calibration.multiplier(), version));
        }
        return new GeneratedPveEnemies(enemies, calibration);
    }

    private static BattlePersonage expectedUnit(boolean front, double m, SkillFormulaVersion version) {
        final var item = new Item(new ItemObject(null, Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(front ? AttackType.SLASH : AttackType.MAGICAL, 1,
                front || version != V3 ? 1 : 2, scaled(front ? 95 : 120, m))),
            Optional.of(new ItemDefense(DefenseType.CLOTH, scaled(front ? 70 : 50, m))),
            scaled(front ? 320 : 270, m), front ? 6 : 10, 8, front ? 0.4 : 0.6,
            front ? 280 : 220, front ? 30 : 20, null),
            Optional.of(new Modifier(front ? ActiveEnum.DOUBLE_ATTACK : ActiveEnum.HIT_AND_RUN)), ItemRarity.LEGENDARY);
        return BattlePersonage.forCombat(List.of(item), front ? Position.FRONT : Position.MID,
            PersonageEffects.EMPTY, Optional.empty(), version);
    }

    private static int scaled(int base, double multiplier) {
        final double value = Math.ceil(base * multiplier);
        Assertions.assertTrue(Double.isFinite(value) && value <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC);
        return Math.max(1, (int) value);
    }

    private static List<Item> playerItems() {
        return List.of(Item.fromObject(new ItemObject(null, Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, 160)),
            Optional.of(new ItemDefense(DefenseType.PLATE, 250)), 1000, 10, 15, 0.4, 300, 10, null)));
    }

    private static List<BattlePersonage> players(int count) {
        return IntStream.range(0, count).mapToObj(index -> BattlePersonage.forCombat(playerItems(),
            Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), V3)).toList();
    }

    private static BattlePersonage powerOnly(double power) {
        final var player = Mockito.mock(BattlePersonage.class);
        Mockito.when(player.equipmentPowerUnscaled()).thenReturn(power);
        return player;
    }

    private static GeneratedPveEnemies generate(List<BattlePersonage> players, double bonus, SkillFormulaVersion version) {
        return RandomUtils.withSeed(SEED, () -> new MaggeeseFlockGenerator().generateCalibrated(players, bonus, version));
    }

    private static Observed observe(List<BattlePersonage> players, double bonus, SkillFormulaVersion version, boolean old) {
        return RandomUtils.withSeed(SEED, () -> new Observed(old ? historical(players, bonus, version)
            : new MaggeeseFlockGenerator().generateCalibrated(players, bonus, version),
            RandomUtils.getInInterval(0L, Long.MAX_VALUE - 1)));
    }

    private static double sum(List<BattlePersonage> people) {
        double result = 0;
        for (final double power : people.stream().mapToDouble(BattlePersonage::equipmentPowerUnscaled).sorted().toArray()) {
            result += power;
        }
        return result;
    }

    private static double group(int count) {
        return count <= 10 ? 1 + MathUtils.log(2.2, count) * 0.05
            : 1 + MathUtils.log(2.2, 10) * 0.05 + MathUtils.log(4, count / 10.0) * 0.05;
    }

    private static IntStream sizes() {
        return IntStream.concat(IntStream.rangeClosed(1, 30), IntStream.of(31, 64, 101));
    }

    private record Observed(GeneratedPveEnemies generated, long nextDraw) { }
}
