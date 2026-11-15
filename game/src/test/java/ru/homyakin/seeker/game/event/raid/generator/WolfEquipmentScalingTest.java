package ru.homyakin.seeker.game.event.raid.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleSkillInitSnapshot;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
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

/** Wolf equipment-power arithmetic and compatibility, not acceptance of a particular win rate. */
class WolfEquipmentScalingTest {
    private static final SkillFormulaVersion V3 = SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final double POWER_BONUS = 2.04;
    private static final double CORRECTION = 0.75;
    private static final long SEED = 2026111516L;

    @ParameterizedTest
    @MethodSource("groupSizes")
    void everySupportedSizeUsesTheSameEquipmentPowerInTrialAndFinalPack(int size) {
        final var players = players(size);
        final var generated = generate(players, POWER_BONUS, V3);

        assertClose(powerSum(players) * POWER_BONUS * groupBonus(size) * CORRECTION,
            generated.calibration().targetPower());
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        Assertions.assertEquals(size + 1, generated.enemies().size());
        Assertions.assertTrue(generated.calibration().evaluations() > 1);
        assertNpcCharacteristics(generated, V3);
        assertDiagnostics(generated, players);
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 32, 63, 64, 101, 128})
    void largerGroupsKeepTheirUncappedWolfCountAndOneAlpha(int size) {
        final var players = players(size);
        final var generated = generate(players, POWER_BONUS, V3);

        Assertions.assertEquals(size + 1, generated.enemies().size());
        assertClose(powerSum(players) * POWER_BONUS * groupBonus(size) * CORRECTION,
            generated.calibration().targetPower());
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        assertNpcCharacteristics(generated, V3);
    }

    @Test
    void newUpperBranchIsAnchoredToThirteenAndUsesRealDivision() {
        final double boundary = 1.0 + MathUtils.log(2.2, 13) * 0.045;
        Assertions.assertEquals(1.1463905683275548, boundary, 1e-15);
        for (final int count : new int[]{1, 12, 13, 14, 15, 25, 26, 30}) {
            final var players = players(count);
            final var generated = generate(players, 1, V3);
            final double actual = generated.calibration().targetPower() / (powerSum(players) * CORRECTION);
            final double expected = count <= 13 ? historicalGroupBonus(count)
                : boundary + MathUtils.log(10, count / 13.0) * 0.045;
            Assertions.assertEquals(expected, actual, 1e-12);
            if (count > 13) {
                Assertions.assertTrue(actual > boundary);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {
        "LEGACY_SKILLS_V1", "SCALING_SKILLS_V1", "SCALING_SKILLS_V2"
    })
    void historicalBranchesRetainExactSearchFactoriesAndRandomDraws(SkillFormulaVersion version) {
        for (final int count : new int[]{1, 3, 13, 14, 31}) {
            final var players = players(count);
            final var expected = RandomUtils.withSeed(SEED, () -> historicalGeneration(players, version));
            final var actual = RandomUtils.withSeed(SEED, () -> {
                final var generated = new WolfPackGenerator().generateCalibrated(players, POWER_BONUS, version);
                return new GenerationState(generated, RandomUtils.getInInterval(0L, Long.MAX_VALUE - 1));
            });

            Assertions.assertEquals(expected.generated().calibration(), actual.generated().calibration());
            Assertions.assertTrue(actual.generated().diagnostics().isEmpty());
            Assertions.assertEquals(expected.nextDraw(), actual.nextDraw());
            for (int index = 0; index < actual.generated().enemies().size(); ++index) {
                final var expectedEnemy = expected.generated().enemies().get(index);
                final var actualEnemy = actual.generated().enemies().get(index);
                Assertions.assertEquals(expectedEnemy.id(), actualEnemy.id());
                Assertions.assertEquals(expectedEnemy.initiativeGauge(), actualEnemy.initiativeGauge());
                Assertions.assertEquals(expectedEnemy.battlePersonageStats(), actualEnemy.battlePersonageStats());
                Assertions.assertEquals(expectedEnemy.skillSnapshots(), actualEnemy.skillSnapshots());
            }
            assertNpcCharacteristics(actual.generated(), version);
        }
    }

    @Test
    void historicalOverloadStillSelectsLegacyAndConsumesTheSameRandomStream() {
        final var players = players(3);
        final var implicit = RandomUtils.withSeed(SEED, () -> new WolfPackGenerator().generate(players, POWER_BONUS));
        final var explicit = generate(players, POWER_BONUS, SkillFormulaVersion.LEGACY_SKILLS_V1);
        Assertions.assertEquals(explicit.enemies().stream().map(BattlePersonage::id).toList(),
            implicit.stream().map(BattlePersonage::id).toList());
        Assertions.assertEquals(explicit.enemies().stream().map(BattlePersonage::battlePersonageStats).toList(),
            implicit.stream().map(BattlePersonage::battlePersonageStats).toList());
    }

    @Test
    void zeroTargetReportsThePositiveMinimumInsteadOfCreatingZeroEnemies() {
        final var generated = generate(players(3), 0, V3);

        Assertions.assertEquals(0, generated.calibration().targetPower());
        Assertions.assertEquals(0, generated.calibration().multiplier());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, generated.calibration().status());
        Assertions.assertTrue(generated.enemies().stream().allMatch(enemy -> enemy.maxHealth() == 1));
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        assertNpcCharacteristics(generated, V3);
    }

    @Test
    void zeroEquipmentPowerParticipantIsAcceptedWithoutLegacyFallback() {
        final var participant = BattlePersonage.forCombat(List.of(), Position.FRONT,
            PersonageEffects.EMPTY, Optional.empty(), V3);
        final var generated = generate(List.of(participant), 1, V3);

        Assertions.assertEquals(0, generated.calibration().targetPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, generated.calibration().status());
        Assertions.assertTrue(generated.calibration().achievedPower() > 0);
        assertNpcCharacteristics(generated, V3);
    }

    @Test
    void laterCombatMutationsAndTacticsDoNotChangeTheEquipmentTarget() {
        final var players = players(3);
        final var unchanged = generate(players, POWER_BONUS, V3);
        for (final var participant : players) {
            participant.increaseAttack(50);
            participant.decreaseDefense(70);
            participant.increaseMaxRange(2, 5);
            participant.setTargetingTactic(TargetingTactic.EXECUTIONER);
        }
        final var changed = generate(players, POWER_BONUS, V3);
        Assertions.assertEquals(unchanged.calibration(), changed.calibration());
        Assertions.assertEquals(unchanged.diagnostics(), changed.diagnostics());
    }

    @Test
    void allSixBasesDefineTheSafeUpperBoundary() {
        final var players = players(3);
        final double multiplier = (PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC - 1000.0) / 1500;
        final double desiredAssessment = powerSum(expectedPack(3, multiplier, V3));
        final double bonus = desiredAssessment / (powerSum(players) * groupBonus(3) * CORRECTION);
        final var generated = generate(players, bonus, V3);

        Assertions.assertTrue(generated.enemies().getFirst().maxHealth()
            > PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC * 0.99);
        assertClose(desiredAssessment, generated.calibration().achievedPower());
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        assertNpcCharacteristics(generated, V3);
    }

    @Test
    void unreachableFiniteTargetFailsInsteadOfSaturatingOrOverflowing() {
        final var players = players(3);
        final double bonus = 1e200 / (powerSum(players) * groupBonus(3) * CORRECTION);
        final var error = Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players, bonus, V3));
        Assertions.assertTrue(error.getMessage().contains("unreachable"));
    }

    @Test
    void permutationHasBitExactTargetsDiagnosticsEnemiesAndRandomness() {
        final var players = List.of(powerOnlyPersonage(1e16), powerOnlyPersonage(1), powerOnlyPersonage(1));
        final var first = generate(players, 1, V3);
        final var reordered = generate(players.reversed(), 1, V3);

        Assertions.assertEquals(10_000_000_000_000_002.0,
            first.diagnostics().orElseThrow().participantAssessment());
        Assertions.assertEquals(first.calibration(), reordered.calibration());
        Assertions.assertEquals(first.diagnostics(), reordered.diagnostics());
        Assertions.assertEquals(first.enemies().stream().map(BattlePersonage::id).toList(),
            reordered.enemies().stream().map(BattlePersonage::id).toList());
        Assertions.assertEquals(first.enemies().stream().map(BattlePersonage::initiativeGauge).toList(),
            reordered.enemies().stream().map(BattlePersonage::initiativeGauge).toList());
        Assertions.assertEquals(first.enemies().stream().map(BattlePersonage::battlePersonageStats).toList(),
            reordered.enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidParticipantPowerIsNotMaskedByLegacyFallback(double power) {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(List.of(powerOnlyPersonage(power)), 1, V3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidBonusIsRejected(double bonus) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), bonus, V3));
    }

    @Test
    void missingGroupEntryAndFormulaVersionAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(null, 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(List.of(), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(Arrays.asList((BattlePersonage) null), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), 1, null));
    }

    @Test
    void overflowingParticipantSumCannotBeHiddenByAZeroBonus() {
        final var players = List.of(powerOnlyPersonage(Double.MAX_VALUE), powerOnlyPersonage(Double.MAX_VALUE));
        final var error = Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players, 0, V3));
        Assertions.assertTrue(error.getMessage().contains("sum"));
    }

    @Test
    void overflowingTargetIsRejectedEvenWhenItsFactorsAreFinite() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), Double.MAX_VALUE, V3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.25, 0.75})
    void searchChoosesTheNearestReachableStep(double fraction) {
        final var players = players(1);
        final double lower = powerSum(expectedPack(1, 0, V3));
        final double upper = powerSum(expectedPack(1, Math.nextUp(1.0 / 1500), V3));
        Assertions.assertTrue(upper > lower);
        final double target = lower + fraction * (upper - lower);
        final var generated = generate(players, target / (powerSum(players) * groupBonus(1) * CORRECTION), V3);

        Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, generated.calibration().status());
        assertClose(fraction < 0.5 ? lower : upper, generated.calibration().achievedPower());
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
    }

    @Test
    void trialSearchCannotConsumeFinalIdentifiersInitiativeOrOuterRandomness() {
        final var minimum = generationRandomness(0);
        final var calibrated = generationRandomness(POWER_BONUS);

        Assertions.assertNotEquals(minimum.evaluations(), calibrated.evaluations());
        Assertions.assertEquals(minimum.identifiers(), calibrated.identifiers());
        Assertions.assertEquals(minimum.initiative(), calibrated.initiative());
        Assertions.assertEquals(minimum.nextDraw(), calibrated.nextDraw());
    }

    private static void assertNpcCharacteristics(GeneratedPveEnemies generated, SkillFormulaVersion version) {
        for (int index = 0; index < generated.enemies().size(); ++index) {
            final boolean alpha = index == 0;
            final var actual = generated.enemies().get(index);
            final var expected = expectedUnit(alpha, generated.calibration().multiplier(), version);
            Assertions.assertEquals(expected.maxHealth(), actual.maxHealth());
            Assertions.assertEquals(expected.attacksByRange(), actual.attacksByRange());
            Assertions.assertEquals(expected.defenses(), actual.defenses());
            Assertions.assertEquals(expected.defenseReductions(), actual.defenseReductions());
            Assertions.assertEquals(alpha ? 15 : 5, actual.critChance());
            Assertions.assertEquals(alpha ? 15 : 5, actual.dodgeChance());
            Assertions.assertEquals(CombatRules.BASE_CRIT_MULTIPLIER + (alpha ? 1 : 0.5), actual.critMultiplier());
            Assertions.assertEquals(alpha ? 200 : 300, actual.initiative());
            Assertions.assertEquals(alpha ? 10 : 30, actual.totalThreat());
            Assertions.assertEquals(Position.FRONT, actual.startPosition());
            Assertions.assertEquals(1, actual.range());
            Assertions.assertEquals(List.of(new BattleSkillInitSnapshot(
                alpha ? ActiveEnum.BERSERK : ActiveEnum.BLEEDING, 4, version
            )), actual.skillSnapshots());
        }
    }

    private static void assertDiagnostics(GeneratedPveEnemies generated, List<BattlePersonage> players) {
        final var diagnostics = generated.diagnostics().orElseThrow();
        Assertions.assertEquals("EQUIPMENT_POWER_SUM_V1", diagnostics.assessmentVersion());
        assertClose(powerSum(players), diagnostics.participantAssessment());
        final var expected = Map.of(
            "enemyCount", (double) players.size() + 1,
            "wolfCount", (double) players.size(),
            "alphaCount", 1.0,
            "powerBonus", POWER_BONUS,
            "groupSizeScaling", groupBonus(players.size()),
            "encounterCorrection", CORRECTION
        );
        Assertions.assertEquals(expected.keySet(), diagnostics.parameters().keySet());
        expected.forEach((key, value) -> Assertions.assertEquals(value, diagnostics.parameters().get(key), 1e-12, key));
    }

    /** Independent replay of the generator before step 16.2 (including two trial creations). */
    private static GenerationState historicalGeneration(List<BattlePersonage> players, SkillFormulaVersion version) {
        final int count = players.size();
        final double target = players.stream().mapToDouble(BattlePersonage::legacyPower).sum()
            * POWER_BONUS * historicalGroupBonus(count);
        final var calibration = PveCharacteristicScaling.search(target,
            PveCharacteristicScaling.maxMultiplier(100, 50, 400, 300, 100, 1500),
            multiplier -> expectedUnit(true, multiplier, version).legacyPower()
                + expectedUnit(false, multiplier, version).legacyPower() * count);
        final var generated = new GeneratedPveEnemies(expectedPack(count, calibration.multiplier(), version), calibration);
        return new GenerationState(generated, RandomUtils.getInInterval(0L, Long.MAX_VALUE - 1));
    }

    private static List<BattlePersonage> expectedPack(int wolfCount, double multiplier, SkillFormulaVersion version) {
        final var pack = new ArrayList<BattlePersonage>();
        pack.add(expectedUnit(true, multiplier, version));
        for (int index = 0; index < wolfCount; ++index) {
            pack.add(expectedUnit(false, multiplier, version));
        }
        return pack;
    }

    private static BattlePersonage expectedUnit(boolean alpha, double multiplier, SkillFormulaVersion version) {
        final var object = new ItemObject(
            null, Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, scaled(alpha ? 300 : 100, multiplier))),
            Optional.of(new ItemDefense(DefenseType.LEATHER, scaled(alpha ? 100 : 50, multiplier))),
            scaled(alpha ? 1500 : 400, multiplier), alpha ? 15 : 5, alpha ? 15 : 5,
            alpha ? 1 : 0.5, alpha ? 200 : 300, alpha ? 10 : 30, null
        );
        final var item = new Item(object,
            Optional.of(new Modifier(alpha ? ActiveEnum.BERSERK : ActiveEnum.BLEEDING)), ItemRarity.LEGENDARY);
        return BattlePersonage.forCombat(List.of(item), Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), version);
    }

    private static int scaled(int base, double multiplier) {
        final double value = Math.ceil(base * multiplier);
        Assertions.assertTrue(Double.isFinite(value));
        Assertions.assertTrue(value <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC);
        return Math.max(1, (int) value);
    }

    private static GeneratedPveEnemies generate(List<BattlePersonage> players, double bonus, SkillFormulaVersion version) {
        return RandomUtils.withSeed(SEED, () -> new WolfPackGenerator().generateCalibrated(players, bonus, version));
    }

    private static IntStream groupSizes() {
        return IntStream.rangeClosed(1, 30);
    }

    private static List<BattlePersonage> players(int count) {
        return IntStream.range(0, count).mapToObj(ignored -> BattlePersonage.forCombat(
            List.of(Item.fromObject(new ItemObject(
                null, Set.of(PersonageSlot.MAIN_HAND),
                List.of(new ItemAttack(AttackType.PIERCE, 1, 1, 100), new ItemAttack(AttackType.MAGICAL, 2, 4, 200)),
                Optional.of(new ItemDefense(DefenseType.LEATHER, 300)),
                1200, 15, 20, 0.4, 300, 10, 0, null, null
            ))),
            Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), V3
        )).toList();
    }

    private static double groupBonus(int count) {
        return count <= 13 ? historicalGroupBonus(count)
            : historicalGroupBonus(13) + MathUtils.log(10, count / 13.0) * 0.045;
    }

    private static double historicalGroupBonus(int count) {
        return count <= 13 ? 1.0 + MathUtils.log(2.2, count) * 0.045
            : 1.1 + MathUtils.log(10, count - 13) * 0.045;
    }

    private static BattlePersonage powerOnlyPersonage(double power) {
        final var participant = Mockito.mock(BattlePersonage.class);
        Mockito.when(participant.equipmentPowerUnscaled()).thenReturn(power);
        return participant;
    }

    private static GenerationRandomness generationRandomness(double bonus) {
        return RandomUtils.withSeed(SEED, () -> {
            final var generated = new WolfPackGenerator().generateCalibrated(players(3), bonus, V3);
            return new GenerationRandomness(
                generated.enemies().stream().map(BattlePersonage::id).toList(),
                generated.enemies().stream().map(BattlePersonage::initiativeGauge).toList(),
                RandomUtils.getInInterval(0L, Long.MAX_VALUE - 1), generated.calibration().evaluations()
            );
        });
    }

    private static double powerSum(List<BattlePersonage> personages) {
        final var powers = personages.stream().mapToDouble(BattlePersonage::equipmentPowerUnscaled).sorted().toArray();
        var sum = 0.0;
        for (final double power : powers) {
            sum += power;
        }
        return sum;
    }

    private static void assertClose(double expected, double actual) {
        Assertions.assertEquals(expected, actual, Math.abs(expected) * 1e-12);
    }

    private record GenerationRandomness(List<UUID> identifiers, List<Integer> initiative, long nextDraw, int evaluations) {
    }

    private record GenerationState(GeneratedPveEnemies generated, long nextDraw) {
    }
}
