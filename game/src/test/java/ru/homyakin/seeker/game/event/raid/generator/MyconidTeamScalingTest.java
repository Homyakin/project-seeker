package ru.homyakin.seeker.game.event.raid.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleSkillInitSnapshot;
import ru.homyakin.seeker.game.battle.BattleTempoVersion;
import ru.homyakin.seeker.game.battle.CombatRules;
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

/** Step 15.6 equipment-power arithmetic and factories, not acceptance of PvE win rates. */
class MyconidTeamScalingTest {
    private static final SkillFormulaVersion V3 = SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final double CORRECTION = 0.82;
    private static final double POWER_BONUS = 2.04;
    private static final long GENERATION_SEED = 20261108152L;

    @ParameterizedTest
    @MethodSource("groupSizes")
    void everySupportedSizeCalibratesTheSameAssessmentInTrialAndFinalFactories(int size) {
        final var players = players(size);
        final var generated = generate(players, POWER_BONUS, V3);
        final int count = Math.max(2, size);
        final double expectedTarget = powerSum(players) * POWER_BONUS * groupBonus(count) * CORRECTION;

        assertClose(expectedTarget, generated.calibration().targetPower());
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        Assertions.assertEquals(count, generated.enemies().size());
        Assertions.assertTrue(generated.calibration().evaluations() > 1);
        Assertions.assertTrue(Double.isFinite(generated.calibration().achievedPower()));
        assertNpcCharacteristics(generated, V3);
        assertDiagnostics(generated, players);
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 32, 63, 64, 101, 128})
    void largerGroupsRetainTheSameSumFormulaAndUncappedEnemyCount(int size) {
        final var players = players(size);
        final var generated = generate(players, POWER_BONUS, V3);
        final double normalized = generated.calibration().targetPower()
            / (powerSum(players) * POWER_BONUS * groupBonus(size) * CORRECTION);

        Assertions.assertEquals(size, generated.enemies().size());
        Assertions.assertEquals(1, normalized, 1e-12);
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        assertNpcCharacteristics(generated, V3);
    }

    @ParameterizedTest
    @CsvSource({"1, 2", "2, 3", "3, 4", "7, 8"})
    void boundaryTargetsUseTheExactCommonFormulaRatherThanAnAdditionalPiecewiseCorrection(int first, int second) {
        for (final int size : new int[]{first, second}) {
            final var players = players(size);
            final var generated = generate(players, 1, V3);
            final int count = Math.max(2, size);
            final double expected = groupBonus(count) * CORRECTION;
            Assertions.assertEquals(expected,
                generated.calibration().targetPower() / powerSum(players), 1e-12);
        }
    }

    @Test
    void oneAndTwoPlayersShareTheTwoEnemyMinimumAndNoExtraSizeCorrection() {
        final var one = players(1);
        final var two = players(2);
        final double oneNormalized = generate(one, 1, V3).calibration().targetPower() / powerSum(one);
        final double twoNormalized = generate(two, 1, V3).calibration().targetPower() / powerSum(two);

        Assertions.assertEquals(oneNormalized, twoNormalized, 1e-12);
        Assertions.assertEquals(groupBonus(2) * CORRECTION, oneNormalized, 1e-12);
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {
        "LEGACY_SKILLS_V1", "SCALING_SKILLS_V1", "SCALING_SKILLS_V2"
    })
    void historicalSelectionsKeepTheOldAssessmentGroupBoundaryAndUnweightedCharacteristics(SkillFormulaVersion version) {
        for (final int size : new int[]{1, 2, 3, 4, 7, 8, 31}) {
            final var players = players(size);
            final var generated = generate(players, POWER_BONUS, version);
            final int count = Math.max(2, size);
            final double group = 1 + 0.05 * MathUtils.log(count <= 7 ? 2.2 : 4, count);
            final double expected = players.stream().mapToDouble(BattlePersonage::legacyPower).sum()
                * POWER_BONUS * group * 0.77;
            final double achieved = generated.enemies().stream().mapToDouble(BattlePersonage::legacyPower).sum();

            assertClose(expected, generated.calibration().targetPower());
            assertClose(achieved, generated.calibration().achievedPower());
            assertNpcCharacteristics(generated, version);
            Assertions.assertTrue(generated.diagnostics().isEmpty());
        }
    }

    @Test
    void theHistoricalOverloadStillUsesLegacyFormulasAndTheOriginalTarget() {
        final var players = players(3);
        final var implicit = RandomUtils.withSeed(GENERATION_SEED,
            () -> new MyconidColonyGenerator().generate(players, POWER_BONUS));
        final var explicit = generate(players, POWER_BONUS, SkillFormulaVersion.LEGACY_SKILLS_V1);

        Assertions.assertEquals(explicit.enemies().size(), implicit.size());
        for (int index = 0; index < implicit.size(); index++) {
            Assertions.assertEquals(explicit.enemies().get(index).battlePersonageStats(),
                implicit.get(index).battlePersonageStats());
            Assertions.assertEquals(explicit.enemies().get(index).skillSnapshots(), implicit.get(index).skillSnapshots());
            Assertions.assertEquals(1, implicit.get(index).range());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void v3SlingersStillOccupyTheActualBackRowInEitherBattleOrder(boolean reverse) {
        final var players = players(3);
        final var enemies = generate(players, 1, V3).enemies();
        final long seed = 30261108152L;
        final var result = RandomUtils.withSeed(seed, () -> new Battle(seed).process(
            reverse ? players : enemies, reverse ? enemies : players, 2000, BattleTempoVersion.ACCELERATED_V1
        ));
        for (int index = 0; index < enemies.size(); index++) {
            final var snapshot = result.initState().personagesById().get(enemies.get(index).id());
            final var position = result.initState().lines().get(snapshot.lineIndex()).position();
            Assertions.assertEquals(index == 0 ? Position.FRONT : Position.BACK, position);
        }
    }

    @Test
    void zeroTargetReturnsTheExplicitMinimumAndStillUsesTheSameFinalAssessment() {
        final var generated = generate(players(3), 0, V3);

        Assertions.assertEquals(0, generated.calibration().targetPower());
        Assertions.assertEquals(0, generated.calibration().multiplier());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, generated.calibration().status());
        Assertions.assertTrue(generated.enemies().stream().allMatch(enemy -> enemy.maxHealth() == 1));
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        assertNpcCharacteristics(generated, V3);
    }

    @Test
    void highFiniteTargetReachesTheUnweightedSafeBoundaryWithoutSaturatingCharacteristics() {
        final var players = players(3);
        final double desiredMultiplier = (PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC - 1000.0) / 420;
        final double desiredAssessment = powerSum(List.of(
            expectedUnit(true, desiredMultiplier, V3),
            expectedUnit(false, desiredMultiplier, V3),
            expectedUnit(false, desiredMultiplier, V3)
        ));
        final double powerBonus = desiredAssessment
            / (powerSum(players) * groupBonus(3) * CORRECTION);
        final var generated = generate(players, powerBonus, V3);

        Assertions.assertTrue(generated.enemies().getFirst().maxHealth()
            > PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC * 0.99);
        assertClose(desiredAssessment, generated.calibration().achievedPower());
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
        assertNpcCharacteristics(generated, V3);
    }

    @Test
    void unreachableFiniteTargetFailsExplicitlyRatherThanOverflowingOrReturningASaturatedEnemy() {
        final var players = players(3);
        final double powerBonus = 1e200
            / (powerSum(players) * groupBonus(3) * CORRECTION);
        Assertions.assertTrue(Double.isFinite(powerBonus));
        final var error = Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players, powerBonus, V3));

        Assertions.assertTrue(error.getMessage().contains("unreachable"));
    }

    @Test
    void permutationHasBitExactTargetsDiagnosticsEnemiesAndRandomnessEvenAcrossMagnitudeDifferences() {
        final var participants = List.of(powerOnlyPersonage(1e16), powerOnlyPersonage(1), powerOnlyPersonage(1));
        final var first = generate(participants, 1, V3);
        final var reordered = generate(participants.reversed(), 1, V3);

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

    @Test
    void zeroEquipmentPowerReturnsTheMinimumRatherThanRejectingAnExistingParticipant() {
        final var participant = BattlePersonage.forCombat(List.of(), Position.FRONT,
            PersonageEffects.EMPTY, Optional.empty(), V3);
        final var generated = generate(List.of(participant), 1, V3);

        Assertions.assertEquals(0, generated.calibration().targetPower());
        Assertions.assertEquals(0, generated.calibration().multiplier());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, generated.calibration().status());
        Assertions.assertTrue(generated.calibration().achievedPower() > 0);
        assertNpcCharacteristics(generated, V3);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidParticipantPowerWithoutFallingBackToLegacy(double power) {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(List.of(powerOnlyPersonage(power)), 1, V3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidLevelBonus(double bonus) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), bonus, V3));
    }

    @Test
    void rejectsNullEmptyAndNullEntryGroupsAndMissingFormulaVersion() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(null, 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(List.of(), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> generate(Arrays.asList((BattlePersonage) null), 1, V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), 1, null));
    }

    @Test
    void rejectsOverflowOfParticipantSumEvenWhenAMultiplierCouldMaskIt() {
        final var participants = List.of(powerOnlyPersonage(Double.MAX_VALUE), powerOnlyPersonage(Double.MAX_VALUE));
        final var error = Assertions.assertThrows(IllegalArgumentException.class, () -> generate(participants, 0, V3));

        Assertions.assertTrue(error.getMessage().contains("sum"));
    }

    @Test
    void rejectsOverflowOfTargetEvenThoughTheBonusAndEachPersonalPowerAreFinite() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> generate(players(1), Double.MAX_VALUE, V3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.25, 0.75})
    void choosesTheNearestReachableNpcStepWithoutDemandingAnExactTarget(double fraction) {
        final var participants = players(1);
        final var lower = powerSum(List.of(expectedUnit(true, 0, V3), expectedUnit(false, 0, V3)));
        final var justAboveFirstStep = Math.nextUp(1.0 / 420);
        final var upper = powerSum(List.of(
            expectedUnit(true, justAboveFirstStep, V3), expectedUnit(false, justAboveFirstStep, V3)));
        Assertions.assertTrue(upper > lower);
        final var target = lower + fraction * (upper - lower);
        final var bonus = target / (powerSum(participants) * groupBonus(2) * CORRECTION);
        final var generated = generate(participants, bonus, V3);

        Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, generated.calibration().status());
        assertClose(fraction < 0.5 ? lower : upper, generated.calibration().achievedPower());
        assertClose(powerSum(generated.enemies()), generated.calibration().achievedPower());
    }

    @Test
    void trialEvaluationCountDoesNotConsumeFinalIdentifiersInitiativeOrOuterRandomDraws() {
        final var minimum = generationRandomness(0);
        final var calibrated = generationRandomness(POWER_BONUS);

        Assertions.assertNotEquals(minimum.evaluations(), calibrated.evaluations());
        Assertions.assertEquals(minimum.identifiers(), calibrated.identifiers());
        Assertions.assertEquals(minimum.initiative(), calibrated.initiative());
        Assertions.assertEquals(minimum.nextDraw(), calibrated.nextDraw());
    }

    private static void assertNpcCharacteristics(
        GeneratedPveEnemies generated, SkillFormulaVersion version
    ) {
        final var enemies = generated.enemies();
        final int count = enemies.size();
        final int front = count / 2;
        for (int index = 0; index < count; index++) {
            final boolean guardian = index < front;
            final var expected = expectedUnit(guardian, generated.calibration().multiplier(), version);
            final var actual = enemies.get(index);

            Assertions.assertEquals(expected.maxHealth(), actual.maxHealth());
            Assertions.assertEquals(expected.attacksByRange(), actual.attacksByRange());
            Assertions.assertEquals(expected.defenses(), actual.defenses());
            Assertions.assertEquals(expected.defenseReductions(), actual.defenseReductions());
            Assertions.assertEquals(expected.critChance(), actual.critChance());
            Assertions.assertEquals(expected.dodgeChance(), actual.dodgeChance());
            Assertions.assertEquals(expected.critMultiplier(), actual.critMultiplier());
            Assertions.assertEquals(guardian ? 190 : 160, actual.initiative());
            Assertions.assertEquals(guardian ? 35 : 15, actual.totalThreat());
            Assertions.assertEquals(guardian ? Position.FRONT : Position.BACK, actual.startPosition());
            Assertions.assertEquals(guardian || version != V3 ? 1 : 3, actual.range());
            Assertions.assertEquals(List.of(new BattleSkillInitSnapshot(
                guardian ? ActiveEnum.KNOCKBACK : ActiveEnum.PRECISE_STRIKE, 4, version
            )), actual.skillSnapshots());
            Assertions.assertEquals(CombatRules.BASE_CRIT_MULTIPLIER + (guardian ? 0.3 : 0.5), actual.critMultiplier());
        }
    }

    private static void assertDiagnostics(GeneratedPveEnemies generated, List<BattlePersonage> players) {
        final var diagnostics = generated.diagnostics().orElseThrow();
        final int count = Math.max(2, players.size());
        final int front = count / 2;
        final var expectedParameters = Map.of(
            "enemyCount", (double) count,
            "frontCount", (double) front,
            "backCount", (double) (count - front),
            "groupSizeScaling", groupBonus(count),
            "powerBonus", POWER_BONUS,
            "encounterCorrection", CORRECTION
        );
        Assertions.assertEquals("EQUIPMENT_POWER_SUM_V1", diagnostics.assessmentVersion());
        assertClose(powerSum(players), diagnostics.participantAssessment());
        Assertions.assertEquals(expectedParameters.keySet(), diagnostics.parameters().keySet());
        expectedParameters.forEach((key, value) ->
            Assertions.assertEquals(value, diagnostics.parameters().get(key), 1e-12, key));
    }

    private static BattlePersonage expectedUnit(boolean guardian, double multiplier, SkillFormulaVersion version) {
        final var object = new ItemObject(
            null, Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.MAGICAL, 1, guardian || version != V3 ? 1 : 3,
                scaled(guardian ? 70 : 130, multiplier))),
            Optional.of(new ItemDefense(DefenseType.ARCANE, scaled(guardian ? 160 : 70, multiplier))),
            scaled(guardian ? 420 : 280, multiplier), guardian ? 4 : 8, guardian ? 3 : 5,
            guardian ? 0.3 : 0.5, guardian ? 190 : 160, guardian ? 35 : 15, null
        );
        final var item = new Item(object,
            Optional.of(new Modifier(guardian ? ActiveEnum.KNOCKBACK : ActiveEnum.PRECISE_STRIKE)), ItemRarity.LEGENDARY);
        return BattlePersonage.forCombat(List.of(item), guardian ? Position.FRONT : Position.BACK,
            PersonageEffects.EMPTY, Optional.empty(), version);
    }

    private static int scaled(int base, double multiplier) {
        final double value = Math.ceil(base * multiplier);
        Assertions.assertTrue(Double.isFinite(value));
        Assertions.assertTrue(value <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC);
        return Math.max(1, (int) value);
    }

    private static GeneratedPveEnemies generate(List<BattlePersonage> players, double powerBonus, SkillFormulaVersion version) {
        return RandomUtils.withSeed(GENERATION_SEED,
            () -> new MyconidColonyGenerator().generateCalibrated(players, powerBonus, version));
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
        return 1 + 0.05 * MathUtils.log(4, count);
    }

    private static BattlePersonage powerOnlyPersonage(double power) {
        final var participant = Mockito.mock(BattlePersonage.class);
        Mockito.when(participant.equipmentPowerUnscaled()).thenReturn(power);
        return participant;
    }

    private static GenerationRandomness generationRandomness(double powerBonus) {
        return RandomUtils.withSeed(GENERATION_SEED, () -> {
            final var generated = new MyconidColonyGenerator().generateCalibrated(players(3), powerBonus, V3);
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
        for (final var power : powers) {
            sum += power;
        }
        return sum;
    }

    private static void assertClose(double expected, double actual) {
        Assertions.assertEquals(expected, actual, Math.abs(expected) * 1e-12);
    }

    private record GenerationRandomness(
        List<java.util.UUID> identifiers,
        List<Integer> initiative,
        long nextDraw,
        int evaluations
    ) {
    }
}
