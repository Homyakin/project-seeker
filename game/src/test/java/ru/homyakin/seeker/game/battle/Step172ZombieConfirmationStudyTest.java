package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step172ZombieConfirmationStudy.Phase;
import ru.homyakin.seeker.game.battle.Step172ZombieConfirmationStudy.Sample;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.EquipmentPowerCalculator;

/** Fixed retry matrix and acceptance arithmetic only; no generated opponents, battles, app or database. */
class Step172ZombieConfirmationStudyTest {
    private static final List<Sample> CASES = Step172ZombieConfirmationStudy.cases(Phase.FINAL);

    @Test
    void oneFixedZombieCorrectionHasTheOriginalCompleteDiagnosticMatrix() {
        Assertions.assertEquals(List.of(RaidType.ZOMBIE_HORDE), Step172ZombieConfirmationStudy.RAIDS);
        Assertions.assertEquals(251, CASES.size());
        Assertions.assertEquals(5900, Step172ZombieConfirmationStudy.battleCount(CASES));
        Assertions.assertEquals(179, CASES.stream().map(Sample::seedGroup).distinct().count());
        Assertions.assertEquals(Map.of("A_MAIN", 33L, "H_LEVELS", 30L, "B_TYPES", 48L,
            "G_WORKABLE", 24L, "E_CAUSAL", 72L, "D_EFFECTS", 24L, "F_DEFAULTS", 20L), CASES.stream()
                .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
        Assertions.assertEquals(Map.of("ZOMBIE_HORDE", 0.76),
            Step172ZombieConfirmationStudy.expectedProductionCorrections());
        for (final var sample : CASES) {
            Assertions.assertEquals(RaidType.ZOMBIE_HORDE.name(), sample.cell().encounter());
            Assertions.assertEquals(Phase.FINAL, sample.phase());
            Assertions.assertEquals(0.76, sample.correction());
            Assertions.assertEquals(0.76, sample.productionCorrection());
            Assertions.assertEquals(sample.cell().size(), sample.members().size());
        }
        final long comparisons = CASES.stream().filter(sample ->
            sample.family().equals("D_EFFECTS") && sample.variant().equals("EFFECTS")
                || sample.family().equals("E_CAUSAL") && !sample.variant().equals("BASE")).count();
        Assertions.assertEquals(72, comparisons);
    }

    @Test
    void allInputsAndConditionsMatchTheFrozenFirstZombieConfirmation() {
        final var first = Step172RaidCalibrationStudy.cases(Step172RaidCalibrationStudy.Phase.FINAL).stream()
            .filter(sample -> sample.cell().encounter().equals(RaidType.ZOMBIE_HORDE.name())).toList();
        Assertions.assertEquals(CASES.size(), first.size());
        for (int index = 0; index < CASES.size(); ++index) {
            final var sample = CASES.get(index);
            final var original = first.get(index);
            Assertions.assertEquals(original.cell(), sample.cell());
            Assertions.assertEquals(original.members(), sample.members());
            Assertions.assertEquals(original.family(), sample.family());
            Assertions.assertEquals(original.variant(), sample.variant());
            Assertions.assertEquals(original.change(), sample.change());
            Assertions.assertEquals(original.pairs(), sample.pairs());
            if (sample.family().equals("A_MAIN")) {
                Assertions.assertEquals("MAIN", sample.cell().style());
                Assertions.assertEquals(10, sample.cell().raidLevel());
                Assertions.assertEquals(50, sample.pairs());
                Assertions.assertFalse(sample.cell().effects());
            }
        }
    }

    @Test
    void everyRequestedBonusIsOnlyTheActualLevelBonusWithoutACorrectionOverride() {
        for (final var sample : CASES) {
            Assertions.assertEquals(1.0 + ((sample.cell().raidLevel() - 10) * 0.06 + 0.14),
                Step172ZombieConfirmationStudy.baseLevelBonus(sample));
            Assertions.assertEquals(Step172ZombieConfirmationStudy.baseLevelBonus(sample),
                Step172ZombieConfirmationStudy.requestedPowerBonus(sample));
        }
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Step172ZombieConfirmationStudy.expectedProductionCorrection(RaidType.WOLFPACK));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> Step172ZombieConfirmationStudy.main(new String[]{"plan-screen", "unused"}));
    }

    @Test
    void freshRootsAreDisjointFromTheScreenAndFirstConfirmationWithFixedPairOffsets() {
        Assertions.assertEquals(20_261_126_001_000L,
            Step172ZombieConfirmationStudy.generationSeed(CASES.getFirst(), 0));
        Assertions.assertEquals(30_261_126_001_000L,
            Step172ZombieConfirmationStudy.battleSeed(CASES.getFirst(), 0));
        for (final var sample : CASES) {
            final long generation = Step172ZombieConfirmationStudy.generationSeed(sample, 0);
            final long battle = Step172ZombieConfirmationStudy.battleSeed(sample, 0);
            Assertions.assertEquals(20_261_126_000_000L + sample.seedGroup() * 1000L, generation);
            Assertions.assertEquals(30_261_126_000_000L + sample.seedGroup() * 1000L, battle);
            Assertions.assertEquals(sample.pairs() - 1,
                Step172ZombieConfirmationStudy.generationSeed(sample, sample.pairs() - 1) - generation);
            Assertions.assertEquals(sample.pairs() - 1,
                Step172ZombieConfirmationStudy.battleSeed(sample, sample.pairs() - 1) - battle);
            Assertions.assertTrue(generation > 20_261_123_716_049L);
            Assertions.assertTrue(battle > 30_261_123_716_049L);
        }
    }

    @Test
    void replayFixesTenCasesAndTwentyMirrorsAcrossAllSevenFamilies() {
        final var replay = Step172ZombieConfirmationStudy.replayCaseIds(CASES);
        Assertions.assertEquals(10, replay.size());
        Assertions.assertEquals(10, replay.stream().distinct().count());
        final var samples = replay.stream().map(id -> CASES.get(id - 1)).toList();
        Assertions.assertEquals(List.of(1, 3, 13, 30), samples.stream()
            .filter(sample -> sample.family().equals("A_MAIN")).map(sample -> sample.cell().size()).toList());
        Assertions.assertEquals(Map.of("A_MAIN", 4L, "B_TYPES", 1L, "G_WORKABLE", 1L, "E_CAUSAL", 1L,
            "D_EFFECTS", 1L, "F_DEFAULTS", 1L, "H_LEVELS", 1L), samples.stream()
                .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
    }

    @Test
    void permanentEquipmentInputsAreLegalAndFinite() {
        for (final var sample : CASES) {
            for (final var member : sample.members()) {
                final var slots = EnumSet.noneOf(PersonageSlot.class);
                for (final var item : member.items()) {
                    item.object().slots().forEach(slot -> Assertions.assertTrue(slots.add(slot)));
                }
                Assertions.assertEquals(EnumSet.allOf(PersonageSlot.class), slots);
                final double power = EquipmentPowerCalculator.calculate(member.items()).unscaled();
                Assertions.assertTrue(Double.isFinite(power) && power > 0);
            }
        }
    }

    @Test
    void acceptanceUsesThirtyThreeContextsNotIndividualTierOrSizeEquality() {
        final var summaries = new ArrayList<Map<String, Object>>();
        for (final var sample : CASES) {
            final int wins = sample.family().equals("A_MAIN") ? switch (sample.cell().tier()) {
                case BEGINNER -> 0;
                case MID -> 50;
                case STRONG -> 100;
                default -> throw new IllegalStateException("Unexpected calibration tier");
            } : 0;
            summaries.add(outcomes(wins));
        }
        final var result = Step172ZombieConfirmationStudy.finalAcceptance(CASES, summaries);
        Assertions.assertEquals("SINGLE_FIXED_ZOMBIE_RETRY_AFTER_FAILED_FIRST_CONFIRMATION", result.get("purpose"));
        Assertions.assertEquals(true, result.get("passesPredeclaredRule"));
        final var raid = (Map<?, ?>) ((Map<?, ?>) result.get("raids")).get("ZOMBIE_HORDE");
        Assertions.assertEquals(33, raid.get("mainContextCount"));
        Assertions.assertEquals(0.5, raid.get("balancedMainWinFraction"));
        Assertions.assertEquals(3300, raid.get("rawMainBattlesNotAcceptanceMetric"));
        Assertions.assertEquals(0.76, raid.get("selectedCorrection"));
    }

    @Test
    void zombieBoundsRemainFortyFiveToFiftyFiveWithoutTheWolfException() {
        for (final int wins : List.of(44, 45, 50, 55, 56)) {
            final var summaries = CASES.stream().map(sample ->
                outcomes(sample.family().equals("A_MAIN") ? wins : 0)).toList();
            final var result = Step172ZombieConfirmationStudy.finalAcceptance(CASES, summaries);
            Assertions.assertEquals(wins >= 45 && wins <= 55, result.get("passesPredeclaredRule"));
        }
    }

    @Test
    void anyDiagnosticTimeoutRejectsTheRetryWithoutSearchingAnotherCorrection() {
        final var summaries = new ArrayList<>(CASES.stream().map(sample ->
            outcomes(sample.family().equals("A_MAIN") ? 50 : 0)).toList());
        final var diagnostic = CASES.stream().filter(sample -> sample.family().equals("H_LEVELS"))
            .findFirst().orElseThrow();
        summaries.set(diagnostic.caseId() - 1, Map.of("outcomes", Map.of("TIMEOUT", 1)));
        final var result = Step172ZombieConfirmationStudy.finalAcceptance(CASES, summaries);
        Assertions.assertEquals(false, result.get("passesPredeclaredRule"));
        final var raid = (Map<?, ?>) ((Map<?, ?>) result.get("raids")).get("ZOMBIE_HORDE");
        Assertions.assertEquals(0.76, raid.get("selectedCorrection"));
        Assertions.assertEquals(1, raid.get("timeoutsAllCases"));
    }

    private static Map<String, Object> outcomes(int wins) {
        return Map.of("outcomes", Map.of("WIN", wins));
    }
}

