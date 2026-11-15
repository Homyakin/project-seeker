package ru.homyakin.seeker.game.event.raid.generator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

class RaidNpcCombatVersionTest {
    @ParameterizedTest
    @EnumSource(RaidType.class)
    void scalingVersionIsUsedByTrialAndFinalFactoriesWithoutDuplicatingPoints(RaidType type) {
        final var players = players(3);
        final var generated = RandomUtils.withSeed(2026110714L, () -> generator(type).generateCalibrated(
            players, 1.14, SkillFormulaVersion.SCALING_SKILLS_V3
        ));

        Assertions.assertEquals(expectedCount(type, players.size()), generated.enemies().size());
        for (final var enemy : generated.enemies()) {
            Assertions.assertEquals(1, enemy.skillSnapshots().size());
            final var skill = enemy.skillSnapshots().getFirst();
            Assertions.assertEquals(4, skill.points(), "One legendary modifier provides its four points exactly once");
            Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V3, skill.formulaVersion());
        }
        final var finalPower = equipmentPowerSum(generated.enemies());
        Assertions.assertEquals(generated.calibration().achievedPower(), finalPower, finalPower * 1e-12);
        Assertions.assertEquals(expectedTarget(type, players, 1.14), generated.calibration().targetPower());
        Assertions.assertTrue(generated.calibration().evaluations() > 1);
        Assertions.assertTrue(Math.abs(finalPower - generated.calibration().targetPower()) / finalPower < 0.005);
        Assertions.assertTrue(generated.diagnostics().isPresent());
    }

    @ParameterizedTest
    @EnumSource(RaidType.class)
    void historicalOverloadStillSelectsLegacyFormulas(RaidType type) {
        final var players = players(3);
        final var implicit = RandomUtils.withSeed(2026110714L, () -> generator(type).generate(players, 1.14));
        final var explicit = RandomUtils.withSeed(2026110714L, () -> generator(type).generate(
            players, 1.14, SkillFormulaVersion.LEGACY_SKILLS_V1
        ));

        Assertions.assertEquals(explicit.size(), implicit.size());
        for (int i = 0; i < implicit.size(); i++) {
            Assertions.assertEquals(explicit.get(i).battlePersonageStats(), implicit.get(i).battlePersonageStats());
            Assertions.assertEquals(explicit.get(i).startPosition(), implicit.get(i).startPosition());
            Assertions.assertEquals(explicit.get(i).attackTypes(), implicit.get(i).attackTypes());
            Assertions.assertEquals(explicit.get(i).skillSnapshots(), implicit.get(i).skillSnapshots());
            Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1,
                implicit.get(i).skillSnapshots().getFirst().formulaVersion());
        }
    }

    @ParameterizedTest
    @EnumSource(RaidType.class)
    void dispatcherKeepsLevelAndGroupCoefficientsAndEnemyCounts(RaidType type) {
        final var dispatcher = new RaidGenerator();
        for (final var size : new int[]{1, 2, 7, 8, 10, 11, 13, 14, 30}) {
            final var players = players(size);
            for (final var level : new int[]{1, 10, 50}) {
                final var generated = dispatcher.generateCalibrated(
                    type, event(level), players, SkillFormulaVersion.SCALING_SKILLS_V3
                );
                final var powerBonus = 1.0 + ((level - 10) * 0.06 + 0.14);
                Assertions.assertEquals(expectedTarget(type, players, powerBonus), generated.calibration().targetPower());
                Assertions.assertEquals(expectedCount(type, size), generated.enemies().size());
                Assertions.assertTrue(generated.enemies().stream().allMatch(enemy ->
                    enemy.skillSnapshots().getFirst().formulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V3
                ));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(RaidType.class)
    void encounterCorrectionIsConstantAcrossGroupsAndLevelsWithoutChangingFinalRandomness(RaidType type) {
        final double correction = switch (type) {
            case WOLFPACK -> 0.75;
            case ZOMBIE_HORDE -> 0.76;
            case MYCONID_COLONY -> 0.82;
            case MAGGEESE_FLOCK -> 0.725;
        };
        Assertions.assertTrue(Double.isFinite(correction) && correction > 0);
        for (final int size : new int[]{1, 3, 10, 11, 13, 14, 30}) {
            final var players = players(size);
            for (final double bonus : new double[]{0.6, 1.14, 2.04, 3.54}) {
                final var actual = observeGeneration(type, players, bonus);
                final var neutral = observeGeneration(type, players, bonus / correction);
                Assertions.assertEquals(correction, actual.generated().diagnostics().orElseThrow()
                    .parameters().get("encounterCorrection"));
                Assertions.assertEquals(expectedTarget(type, players, bonus),
                    actual.generated().calibration().targetPower());
                Assertions.assertEquals(equipmentPowerSum(actual.generated().enemies()),
                    actual.generated().calibration().achievedPower());
                Assertions.assertEquals(neutral.nextDraw(), actual.nextDraw());
                Assertions.assertEquals(neutral.generated().enemies().stream().map(BattlePersonage::id).toList(),
                    actual.generated().enemies().stream().map(BattlePersonage::id).toList());
                Assertions.assertEquals(neutral.generated().enemies().stream()
                        .map(BattlePersonage::initiativeGauge).toList(),
                    actual.generated().enemies().stream().map(BattlePersonage::initiativeGauge).toList());
                Assertions.assertEquals(neutral.generated().enemies().stream()
                        .map(BattlePersonage::attackTypes).toList(),
                    actual.generated().enemies().stream().map(BattlePersonage::attackTypes).toList());
                Assertions.assertEquals(neutral.generated().enemies().stream()
                        .map(BattlePersonage::skillSnapshots).toList(),
                    actual.generated().enemies().stream().map(BattlePersonage::skillSnapshots).toList());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(RaidType.class)
    void minimumAndInvalidInputsAreExplicit(RaidType type) {
        final var generator = generator(type);
        final var players = players(1);
        final var minimum = generator.generateCalibrated(players, 0, SkillFormulaVersion.SCALING_SKILLS_V3);
        Assertions.assertEquals(0, minimum.calibration().multiplier());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, minimum.calibration().status());
        Assertions.assertTrue(minimum.enemies().stream().allMatch(enemy -> enemy.maxHealth() == 1));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            generator.generate(List.of(), 1, SkillFormulaVersion.SCALING_SKILLS_V3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> generator.generate(players, 1, null));
        for (final var powerBonus : new double[]{-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                generator.generate(players, powerBonus, SkillFormulaVersion.SCALING_SKILLS_V3));
        }
    }

    @Test
    void dispatcherHistoricalOverloadRetainsLegacyVersion() {
        final var generated = new RaidGenerator().generate(RaidType.ZOMBIE_HORDE, event(10), players(3));
        Assertions.assertTrue(generated.stream().allMatch(enemy ->
            enemy.skillSnapshots().getFirst().formulaVersion() == SkillFormulaVersion.LEGACY_SKILLS_V1
        ));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 51, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void equipmentPowerScaledRaidsRejectRaidLevelsOutsideTheExistingDomain(int level) {
        for (final var type : RaidType.values()) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new RaidGenerator().generateCalibrated(
                type, event(level), players(3), SkillFormulaVersion.SCALING_SKILLS_V3
            ));
        }
    }

    @Test
    void theNewLevelGuardDoesNotChangeHistoricalRaidDispatch() {
        final var dispatcher = new RaidGenerator();
        for (final var type : RaidType.values()) {
            for (final var version : List.of(SkillFormulaVersion.LEGACY_SKILLS_V1,
                SkillFormulaVersion.SCALING_SKILLS_V1, SkillFormulaVersion.SCALING_SKILLS_V2)) {
                Assertions.assertTrue(dispatcher.generateCalibrated(
                    type, event(51), players(3), version
                ).diagnostics().isEmpty());
            }
        }
    }

    private static RaidBattlePersonageGenerator generator(RaidType type) {
        return switch (type) {
            case WOLFPACK -> new WolfPackGenerator();
            case ZOMBIE_HORDE -> new ZombieHordeGenerator();
            case MYCONID_COLONY -> new MyconidColonyGenerator();
            case MAGGEESE_FLOCK -> new MaggeeseFlockGenerator();
        };
    }

    private static int expectedCount(RaidType type, int playerCount) {
        return switch (type) {
            case WOLFPACK, ZOMBIE_HORDE -> playerCount + 1;
            case MYCONID_COLONY, MAGGEESE_FLOCK -> Math.max(2, playerCount);
        };
    }

    private static double expectedTarget(RaidType type, List<BattlePersonage> players, double powerBonus) {
        final var size = players.size();
        if (type == RaidType.MYCONID_COLONY) {
            final var count = Math.max(2, size);
            return equipmentPowerSum(players) * powerBonus
                * (1 + MathUtils.log(4, count) * 0.05) * 0.82;
        }
        final double groupBonus;
        switch (type) {
            case WOLFPACK -> groupBonus = size <= 13
                ? 1.0 + MathUtils.log(2.2, size) * 0.045
                : (1.0 + MathUtils.log(2.2, 13) * 0.045) + MathUtils.log(10, size / 13.0) * 0.045;
            case ZOMBIE_HORDE -> {
                final double at10 = 1.0 + MathUtils.log(2.2, 10) * 0.05;
                final double at13 = at10 + MathUtils.log(4, 13 / 10.0) * 0.05;
                groupBonus = size <= 10 ? 1.0 + MathUtils.log(2.2, size) * 0.05
                    : size <= 13 ? at10 + MathUtils.log(4, size / 10.0) * 0.05
                    : at13 + MathUtils.log(8, size / 13.0) * 0.05;
            }
            case MAGGEESE_FLOCK -> {
                final int count = Math.max(2, size);
                groupBonus = count <= 10 ? 1.0 + MathUtils.log(2.2, count) * 0.05
                    : (1.0 + MathUtils.log(2.2, 10) * 0.05) + MathUtils.log(4, count / 10.0) * 0.05;
            }
            default -> throw new IllegalStateException("Unexpected raid type");
        }
        final var baseTarget = equipmentPowerSum(players) * powerBonus * groupBonus;
        return switch (type) {
            case ZOMBIE_HORDE -> baseTarget * 0.76;
            case MAGGEESE_FLOCK -> baseTarget * 0.725;
            default -> baseTarget * 0.75;
        };
    }

    private static ObservedGeneration observeGeneration(RaidType type, List<BattlePersonage> players, double bonus) {
        return RandomUtils.withSeed(20_261_122_172L, () -> new ObservedGeneration(
            generator(type).generateCalibrated(players, bonus, SkillFormulaVersion.SCALING_SKILLS_V3),
            RandomUtils.getInInterval(0L, Long.MAX_VALUE - 1)));
    }

    private static LaunchedRaidEvent event(int level) {
        final var start = LocalDateTime.of(2026, 11, 7, 14, 0);
        return new LaunchedRaidEvent(14, 1, start, start.plusHours(1), EventStatus.LAUNCHED, new RaidParams(level, 0));
    }

    private static double equipmentPowerSum(List<BattlePersonage> personages) {
        return personages.stream().mapToDouble(BattlePersonage::equipmentPowerUnscaled)
            .sorted().reduce(0.0, Double::sum);
    }

    private static List<BattlePersonage> players(int size) {
        return IntStream.range(0, size).mapToObj(ignored -> new BattlePersonage(List.of(Item.fromObject(
            new ItemObject(
                null,
                Set.of(PersonageSlot.MAIN_HAND),
                Optional.of(new ItemAttack(AttackType.SLASH, 1, 1000)),
                Optional.of(new ItemDefense(DefenseType.LEATHER, 500)),
                5000, 5, 5, 0.5, 300, 10, null
            )
        )), Position.FRONT)).toList();
    }

    private record ObservedGeneration(GeneratedPveEnemies generated, long nextDraw) {
    }
}
