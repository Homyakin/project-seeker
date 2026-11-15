package ru.homyakin.seeker.game.event.raid.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleSkillInitSnapshot;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
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
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

/** Exact equipment matching and compatibility, not a target win rate. */
class ZombieEquipmentScalingTest {
    private static final SkillFormulaVersion V3 = SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final long SEED = 2026111517L;
    private static final double CORRECTION = 0.76;

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 7, 9, 10, 11, 12, 13, 14, 15, 30, 31, 64})
    void sumsActualFinalFactoriesAndUsesContinuousGroupScaling(int count) {
        final var players = players(count);
        final var generated = generate(players, 2.04, V3).generated();
        Assertions.assertEquals(power(players) * 2.04 * group(count) * CORRECTION,
            generated.calibration().targetPower());
        Assertions.assertEquals(power(generated.enemies()), generated.calibration().achievedPower());
        Assertions.assertEquals(count + 1, generated.enemies().size());
        final var diagnostics = generated.diagnostics().orElseThrow();
        Assertions.assertEquals("EQUIPMENT_POWER_SUM_V1", diagnostics.assessmentVersion());
        Assertions.assertEquals(power(players), diagnostics.participantAssessment());
        Assertions.assertEquals(group(count), diagnostics.parameters().get("groupSizeScaling"));
        Assertions.assertEquals(CORRECTION, diagnostics.parameters().get("encounterCorrection"));
        for (int index = 0; index < generated.enemies().size(); ++index) {
            final var actual = generated.enemies().get(index);
            final var type = actual.attackTypes().iterator().next();
            final var expected = unit(index == 0, generated.calibration().multiplier(), type, V3);
            Assertions.assertTrue(type == AttackType.SLASH || type == AttackType.BLUNT);
            Assertions.assertEquals(expected.maxHealth(), actual.maxHealth());
            Assertions.assertEquals(expected.attacksByRange(), actual.attacksByRange());
            Assertions.assertEquals(expected.defenses(), actual.defenses());
            Assertions.assertEquals(expected.critChance(), actual.critChance());
            Assertions.assertEquals(expected.dodgeChance(), actual.dodgeChance());
            Assertions.assertEquals(expected.critMultiplier(), actual.critMultiplier());
            Assertions.assertEquals(index == 0 ? 150 : 200, actual.initiative());
            Assertions.assertEquals(Position.FRONT, actual.startPosition());
            Assertions.assertEquals(1, actual.range());
            Assertions.assertEquals(List.of(new BattleSkillInitSnapshot(
                index == 0 ? ActiveEnum.SELF_HEAL : ActiveEnum.THORNS, 4, V3)), actual.skillSnapshots());
        }
    }

    @Test
    void groupScalingNeverDropsAtEitherBoundaryAndKeepsTheLowerBranch() {
        var previous = 0.0;
        for (int count = 1; count <= 100; ++count) {
            final var generated = generate(players(count), 1, V3).generated();
            final var actual = generated.diagnostics().orElseThrow().parameters().get("groupSizeScaling");
            Assertions.assertTrue(actual >= previous);
            if (count <= 10) {
                Assertions.assertEquals(1 + MathUtils.log(2.2, count) * 0.05, actual);
            }
            previous = actual;
        }
    }

    @Test
    void oneFiniteCorrectionAppliesToEveryGroupAndLevelWithoutChangingFinalRandomness() {
        for (final int count : new int[]{1, 3, 10, 11, 13, 14, 30}) {
            final var players = players(count);
            for (final double bonus : new double[]{0.6, 1.14, 2.04, 3.54}) {
                final var corrected = generate(players, bonus, V3);
                final var withoutCorrection = generate(players, bonus / CORRECTION, V3);
                final double correction = corrected.generated().diagnostics().orElseThrow()
                    .parameters().get("encounterCorrection");

                Assertions.assertTrue(Double.isFinite(correction) && correction > 0);
                Assertions.assertEquals(CORRECTION, correction);
                Assertions.assertEquals(power(players) * bonus * group(count) * CORRECTION,
                    corrected.generated().calibration().targetPower());
                assertSameRandomness(withoutCorrection, corrected);
                Assertions.assertTrue(corrected.generated().enemies().getFirst().maxHealth()
                    < withoutCorrection.generated().enemies().getFirst().maxHealth());
                Assertions.assertTrue(corrected.generated().calibration().achievedPower()
                    < withoutCorrection.generated().calibration().achievedPower());
                for (int index = 0; index < corrected.generated().enemies().size(); ++index) {
                    final var actual = corrected.generated().enemies().get(index);
                    final var baseline = withoutCorrection.generated().enemies().get(index);
                    Assertions.assertTrue(actual.maxHealth() <= baseline.maxHealth());
                    Assertions.assertTrue(actual.battlePersonageStats().attack() <= baseline.battlePersonageStats().attack());
                }
            }
        }
    }

    @Test
    void attackVectorIsDrawnOnceBeforeFinalIdentifiersAndIndependentOfSearchLength() {
        final var players = players(14);
        final var minimum = generate(players, 0, V3);
        final var calibrated = generate(players, 2.04, V3);
        Assertions.assertNotEquals(minimum.generated().calibration().evaluations(),
            calibrated.generated().calibration().evaluations());
        assertSameRandomness(minimum, calibrated);

        final var expected = RandomUtils.withSeed(SEED, () -> {
            final var attacks = new ArrayList<AttackType>();
            for (int index = 0; index < 15; ++index) {
                attacks.add(randomAttack());
            }
            final var enemies = new ArrayList<BattlePersonage>();
            for (int index = 0; index < attacks.size(); ++index) {
                enemies.add(unit(index == 0, calibrated.generated().calibration().multiplier(), attacks.get(index), V3));
            }
            return new RandomState(new GeneratedPveEnemies(enemies, calibrated.generated().calibration()),
                RandomUtils.getWithMax(1_000_000));
        });
        assertSameRandomness(expected, calibrated);
        Assertions.assertEquals(expected.generated().enemies().stream().map(BattlePersonage::battlePersonageStats).toList(),
            calibrated.generated().enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
        Assertions.assertEquals(2, calibrated.generated().enemies().stream()
            .flatMap(enemy -> enemy.attackTypes().stream()).distinct().count());
    }

    @Test
    void ownAttackTypeDoesNotChangeNeutralEquipmentPower() {
        for (final var multiplier : new double[]{0, 0.1, 1, 100}) {
            for (final boolean boss : new boolean[]{false, true}) {
                Assertions.assertEquals(unit(boss, multiplier, AttackType.SLASH, V3).equipmentPowerUnscaled(),
                    unit(boss, multiplier, AttackType.BLUNT, V3).equipmentPowerUnscaled());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {
        "LEGACY_SKILLS_V1", "SCALING_SKILLS_V1", "SCALING_SKILLS_V2"
    })
    void historicalPathKeepsTargetFactoriesAndTheWholeRandomStream(SkillFormulaVersion version) {
        for (final int count : new int[]{1, 10, 11, 13, 14, 30}) {
            final var players = players(count);
            final var actual = generate(players, 1.14, version);
            final var expected = RandomUtils.withSeed(SEED, () -> {
                final var oldGroup = 1 + MathUtils.log(count <= 10 ? 2.2 : count <= 13 ? 4 : 8, count) * 0.05;
                final var target = players.stream().mapToDouble(BattlePersonage::legacyPower).sum() * 1.14 * oldGroup;
                final var calibration = PveCharacteristicScaling.search(target,
                    PveCharacteristicScaling.maxMultiplier(80, 200, 500, 180, 350, 2000),
                    multiplier -> unit(true, multiplier, AttackType.SLASH, version).legacyPower()
                        + unit(false, multiplier, AttackType.SLASH, version).legacyPower() * count);
                final var enemies = new ArrayList<BattlePersonage>();
                enemies.add(unit(true, calibration.multiplier(), randomAttack(), version));
                for (int index = 0; index < count; ++index) {
                    enemies.add(unit(false, calibration.multiplier(), randomAttack(), version));
                }
                return new RandomState(new GeneratedPveEnemies(enemies, calibration), RandomUtils.getWithMax(1_000_000));
            });
            Assertions.assertEquals(expected.generated().calibration(), actual.generated().calibration());
            Assertions.assertTrue(actual.generated().diagnostics().isEmpty());
            assertSameRandomness(expected, actual);
            Assertions.assertEquals(expected.generated().enemies().stream().map(BattlePersonage::battlePersonageStats).toList(),
                actual.generated().enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
        }
    }

    @Test
    void zeroAndUnreachableTargetsAndInvalidInputsAreExplicit() {
        final var players = players(3);
        final var minimum = generate(players, 0, V3).generated();
        Assertions.assertEquals(0, minimum.calibration().multiplier());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, minimum.calibration().status());
        Assertions.assertTrue(minimum.enemies().stream().allMatch(enemy -> enemy.maxHealth() == 1));
        Assertions.assertEquals(power(minimum.enemies()), minimum.calibration().achievedPower());
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players, 1e200, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players, Double.MAX_VALUE, V3));
        for (final double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players, value, V3));
            Assertions.assertThrows(IllegalArgumentException.class, () -> generate(List.of(powerOnly(value)), 1, V3));
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(List.of(), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(null, 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(Arrays.asList((BattlePersonage) null), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players, 1, null));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(List.of(powerOnly(Double.MAX_VALUE), powerOnly(Double.MAX_VALUE)), 0, V3));
    }

    @Test
    void aHighFiniteTargetReachesTheSafeCharacteristicBoundaryWithoutSaturation() {
        final var players = players(3);
        final var multiplier = (PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC - 1000.0) / 2000;
        final var expectedPower = power(List.of(unit(true, multiplier, AttackType.SLASH, V3),
            unit(false, multiplier, AttackType.BLUNT, V3), unit(false, multiplier, AttackType.SLASH, V3),
            unit(false, multiplier, AttackType.BLUNT, V3)));
        final var bonus = expectedPower / (power(players) * group(3) * CORRECTION);
        final var actual = generate(players, bonus, V3).generated();
        Assertions.assertTrue(actual.enemies().getFirst().maxHealth()
            > PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC * 0.99);
        Assertions.assertEquals(power(actual.enemies()), actual.calibration().achievedPower());
        Assertions.assertEquals(expectedPower, actual.calibration().achievedPower(), expectedPower * 1e-12);
        Assertions.assertTrue(actual.enemies().stream().allMatch(enemy ->
            enemy.maxHealth() <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC));
    }

    @Test
    void permutationUsesTheSameSortedSumAndDoesNotAlterAttackMixOrRandomness() {
        final var players = List.of(powerOnly(1e16), powerOnly(1), powerOnly(1));
        final var first = generate(players, 1, V3);
        final var reversed = generate(players.reversed(), 1, V3);
        Assertions.assertEquals(10_000_000_000_000_002.0,
            first.generated().diagnostics().orElseThrow().participantAssessment());
        Assertions.assertEquals(first.generated().calibration(), reversed.generated().calibration());
        Assertions.assertEquals(first.generated().diagnostics(), reversed.generated().diagnostics());
        assertSameRandomness(first, reversed);
    }

    private static double group(int count) {
        final double atTen = 1 + MathUtils.log(2.2, 10) * 0.05;
        if (count <= 10) {
            return 1 + MathUtils.log(2.2, count) * 0.05;
        }
        if (count <= 13) {
            return atTen + MathUtils.log(4, count / 10.0) * 0.05;
        }
        return (atTen + MathUtils.log(4, 13 / 10.0) * 0.05) + MathUtils.log(8, count / 13.0) * 0.05;
    }

    private static RandomState generate(List<BattlePersonage> players, double bonus, SkillFormulaVersion version) {
        return RandomUtils.withSeed(SEED, () -> new RandomState(
            new ZombieHordeGenerator().generateCalibrated(players, bonus, version), RandomUtils.getWithMax(1_000_000)));
    }

    private static void assertSameRandomness(RandomState expected, RandomState actual) {
        Assertions.assertEquals(expected.nextDraw(), actual.nextDraw());
        Assertions.assertEquals(expected.generated().enemies().stream().map(BattlePersonage::id).toList(),
            actual.generated().enemies().stream().map(BattlePersonage::id).toList());
        Assertions.assertEquals(expected.generated().enemies().stream().map(BattlePersonage::initiativeGauge).toList(),
            actual.generated().enemies().stream().map(BattlePersonage::initiativeGauge).toList());
        Assertions.assertEquals(expected.generated().enemies().stream().map(BattlePersonage::attackTypes).toList(),
            actual.generated().enemies().stream().map(BattlePersonage::attackTypes).toList());
    }

    private static BattlePersonage unit(boolean boss, double multiplier, AttackType type, SkillFormulaVersion version) {
        final var item = new Item(new ItemObject(null, Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(type, 1, scaled(boss ? 180 : 80, multiplier))),
            Optional.of(new ItemDefense(DefenseType.PLATE, scaled(boss ? 350 : 200, multiplier))),
            scaled(boss ? 2000 : 500, multiplier), boss ? 8 : 3, boss ? 3 : 2,
            boss ? 0.5 : 0.3, boss ? 150 : 200, boss ? 50 : 25, null),
            Optional.of(new Modifier(boss ? ActiveEnum.SELF_HEAL : ActiveEnum.THORNS)), ItemRarity.LEGENDARY);
        return BattlePersonage.forCombat(List.of(item), Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), version);
    }

    private static int scaled(int base, double multiplier) {
        return Math.max(1, (int) Math.ceil(base * multiplier));
    }

    private static AttackType randomAttack() {
        return RandomUtils.processChance(50) ? AttackType.BLUNT : AttackType.SLASH;
    }

    private static List<BattlePersonage> players(int count) {
        return IntStream.range(0, count).mapToObj(ignored -> BattlePersonage.forCombat(List.of(Item.fromObject(
            new ItemObject(null, Set.of(PersonageSlot.MAIN_HAND), Optional.of(new ItemAttack(AttackType.SLASH, 1, 1000)),
                Optional.of(new ItemDefense(DefenseType.LEATHER, 500)), 5000, 5, 5, 0.5, 300, 10, null))),
            Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), V3)).toList();
    }

    private static BattlePersonage powerOnly(double value) {
        final var result = Mockito.mock(BattlePersonage.class);
        Mockito.when(result.equipmentPowerUnscaled()).thenReturn(value);
        return result;
    }

    private static double power(List<BattlePersonage> values) {
        return values.stream().mapToDouble(BattlePersonage::equipmentPowerUnscaled).sorted().reduce(0, Double::sum);
    }

    private record RandomState(GeneratedPveEnemies generated, int nextDraw) {
    }
}
