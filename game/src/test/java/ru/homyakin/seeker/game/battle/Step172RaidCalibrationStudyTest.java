package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step172RaidCalibrationStudy.Phase;
import ru.homyakin.seeker.game.battle.Step172RaidCalibrationStudy.Sample;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.EquipmentPowerCalculator;

/** Matrices and predeclared decision arithmetic only; no generation, battles, app or database startup. */
class Step172RaidCalibrationStudyTest {
    private static final List<Sample> SCREEN = Step172RaidCalibrationStudy.cases(Phase.SCREEN);
    private static final List<Sample> FINAL = Step172RaidCalibrationStudy.cases(Phase.FINAL);

    @Test
    void screenHasFourFixedGridsAndTheSameThirtyThreeMainConditionsForEveryRaid() {
        Assertions.assertEquals(825, SCREEN.size());
        Assertions.assertEquals(16500, Step172RaidCalibrationStudy.battleCount(SCREEN));
        Assertions.assertEquals(132, SCREEN.stream().map(Sample::seedGroup).distinct().count());
        for (final var raid : Step172RaidCalibrationStudy.RAIDS) {
            final var samples = SCREEN.stream().filter(sample -> sample.cell().encounter().equals(raid.name())).toList();
            Assertions.assertEquals(33 * Step172RaidCalibrationStudy.CANDIDATES.get(raid).size(), samples.size());
            final var groups = samples.stream().collect(Collectors.groupingBy(Sample::seedGroup));
            Assertions.assertEquals(33, groups.size());
            for (final var group : groups.values()) {
                Assertions.assertEquals(Step172RaidCalibrationStudy.CANDIDATES.get(raid),
                    group.stream().map(Sample::correction).toList());
                Assertions.assertEquals(1, group.stream().filter(sample -> sample.variant().equals("BASE")).count());
                for (final var sample : group) {
                    Assertions.assertEquals("S_MAIN", sample.family());
                    Assertions.assertEquals("MAIN", sample.cell().style());
                    Assertions.assertEquals(10, sample.cell().raidLevel());
                    Assertions.assertEquals(10, sample.pairs());
                    Assertions.assertFalse(sample.cell().effects());
                    Assertions.assertEquals(group.getFirst().members(), sample.members());
                    Assertions.assertEquals(group.getFirst().cell(), sample.cell());
                    Assertions.assertEquals(Step172RaidCalibrationStudy.generationSeed(group.getFirst(), 0),
                        Step172RaidCalibrationStudy.generationSeed(sample, 0));
                    Assertions.assertEquals(Step172RaidCalibrationStudy.battleSeed(group.getFirst(), 0),
                        Step172RaidCalibrationStudy.battleSeed(sample, 0));
                }
            }
        }
    }

    @Test
    void holdoutIsUniformAcrossRaidsWithPredeclaredDiagnosticsAndMatchedControls() {
        Assertions.assertEquals(1004, FINAL.size());
        Assertions.assertEquals(23600, Step172RaidCalibrationStudy.battleCount(FINAL));
        Assertions.assertEquals(716, FINAL.stream().map(Sample::seedGroup).distinct().count());
        for (final var raid : Step172RaidCalibrationStudy.RAIDS) {
            final var samples = FINAL.stream().filter(sample -> sample.cell().encounter().equals(raid.name())).toList();
            Assertions.assertEquals(251, samples.size());
            Assertions.assertEquals(5900, Step172RaidCalibrationStudy.battleCount(samples));
            Assertions.assertEquals(179, samples.stream().map(Sample::seedGroup).distinct().count());
            Assertions.assertEquals(Map.of("A_MAIN", 33L, "H_LEVELS", 30L, "B_TYPES", 48L,
                "G_WORKABLE", 24L, "E_CAUSAL", 72L, "D_EFFECTS", 24L, "F_DEFAULTS", 20L), samples.stream()
                    .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
            for (final var sample : samples) {
                Assertions.assertEquals(sample.productionCorrection(), sample.correction());
                if (sample.family().equals("A_MAIN")) {
                    Assertions.assertEquals(50, sample.pairs());
                    Assertions.assertEquals(10, sample.cell().raidLevel());
                    Assertions.assertTrue(Step172RaidCalibrationStudy.SIZES.contains(sample.cell().size()));
                }
                if (sample.family().equals("H_LEVELS")) {
                    Assertions.assertEquals(Tier.MID, sample.cell().tier());
                    Assertions.assertTrue(List.of(1, 3, 7, 13, 30).contains(sample.cell().size()));
                    Assertions.assertTrue(List.of(1, 5, 15, 20, 25, 50).contains(sample.cell().raidLevel()));
                    Assertions.assertEquals(10, sample.pairs());
                }
            }
            final var effectGroups = samples.stream().filter(sample -> sample.family().equals("D_EFFECTS"))
                .collect(Collectors.groupingBy(Sample::seedGroup));
            Assertions.assertEquals(12, effectGroups.size());
            for (final var group : effectGroups.values()) {
                Assertions.assertEquals(List.of("BASE", "EFFECTS"), group.stream().map(Sample::variant).toList());
                Assertions.assertEquals(group.getFirst().members(), group.getLast().members());
                Assertions.assertFalse(group.getFirst().cell().effects());
                Assertions.assertTrue(group.getLast().cell().effects());
            }
            final var causalGroups = samples.stream().filter(sample -> sample.family().equals("E_CAUSAL"))
                .collect(Collectors.groupingBy(Sample::seedGroup));
            Assertions.assertEquals(12, causalGroups.size());
            causalGroups.values().forEach(group -> Assertions.assertEquals(List.of("BASE", "NORMAL_APPEND",
                "WEAK_APPEND", "ONE_ENHANCE", "ONE_RARITY", "UNEQUIP_ARMOR"),
                group.stream().map(Sample::variant).toList()));
        }
    }

    @Test
    void freshPhaseSeedsAndFixedReplayCoverEveryRaidAndDiagnosticFamily() {
        Assertions.assertEquals(20_261_122_001_000L, Step172RaidCalibrationStudy.generationSeed(SCREEN.getFirst(), 0));
        Assertions.assertEquals(30_261_122_001_000L, Step172RaidCalibrationStudy.battleSeed(SCREEN.getFirst(), 0));
        Assertions.assertEquals(20_261_123_001_000L, Step172RaidCalibrationStudy.generationSeed(FINAL.getFirst(), 0));
        Assertions.assertEquals(30_261_123_001_000L, Step172RaidCalibrationStudy.battleSeed(FINAL.getFirst(), 0));
        Assertions.assertTrue(Step172RaidCalibrationStudy.replayCaseIds(SCREEN).isEmpty());
        final var replay = Step172RaidCalibrationStudy.replayCaseIds(FINAL);
        Assertions.assertEquals(40, replay.size());
        Assertions.assertEquals(40, replay.stream().distinct().count());
        for (final var raid : Step172RaidCalibrationStudy.RAIDS) {
            final var samples = replay.stream().map(id -> FINAL.get(id - 1))
                .filter(sample -> sample.cell().encounter().equals(raid.name())).toList();
            Assertions.assertEquals(10, samples.size());
            Assertions.assertEquals(List.of(1, 3, 13, 30), samples.stream()
                .filter(sample -> sample.family().equals("A_MAIN")).map(sample -> sample.cell().size()).toList());
            Assertions.assertEquals(Map.of("A_MAIN", 4L, "B_TYPES", 1L, "G_WORKABLE", 1L, "E_CAUSAL", 1L,
                "D_EFFECTS", 1L, "F_DEFAULTS", 1L, "H_LEVELS", 1L), samples.stream()
                    .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
        }
    }

    @Test
    void screenOverrideIsExplicitButFinalUsesOnlyTheActualLevelBonus() {
        for (final var sample : SCREEN) {
            Assertions.assertEquals(Step172RaidCalibrationStudy.baseLevelBonus(sample) * sample.correction()
                / sample.productionCorrection(), Step172RaidCalibrationStudy.requestedPowerBonus(sample));
        }
        for (final var sample : FINAL) {
            Assertions.assertEquals(Step172RaidCalibrationStudy.baseLevelBonus(sample),
                Step172RaidCalibrationStudy.requestedPowerBonus(sample));
        }
    }

    @Test
    void actualInputsRemainLegalAndHaveFinitePermanentEquipmentPower() {
        for (final var sample : java.util.stream.Stream.concat(SCREEN.stream(), FINAL.stream()).toList()) {
            for (final var member : sample.members()) {
                final var slots = EnumSet.noneOf(PersonageSlot.class);
                for (final var item : member.items()) {
                    item.object().slots().forEach(slot -> Assertions.assertTrue(slots.add(slot)));
                }
                Assertions.assertEquals(EnumSet.allOf(PersonageSlot.class), slots);
                final var power = EquipmentPowerCalculator.calculate(member.items()).unscaled();
                Assertions.assertTrue(Double.isFinite(power) && power > 0);
            }
        }
    }

    @Test
    void selectionChoosesClosestToHalfPerRaidAndTimeoutCannotTriggerFallbackTuning() {
        final var summaries = new ArrayList<Map<String, Object>>();
        for (final var sample : SCREEN) {
            final var grid = Step172RaidCalibrationStudy.CANDIDATES.get(RaidType.valueOf(sample.cell().encounter()));
            final int index = grid.indexOf(sample.correction());
            summaries.add(outcomes(index == 1 ? 10 : index == 0 ? 8 : 14));
        }
        final var result = Step172RaidCalibrationStudy.selection(SCREEN, summaries);
        Assertions.assertEquals(true, result.get("passesPredeclaredRule"));
        final var raids = (Map<?, ?>) result.get("raids");
        for (final var raid : Step172RaidCalibrationStudy.RAIDS) {
            final var entry = (Map<?, ?>) raids.get(raid.name());
            Assertions.assertEquals(Step172RaidCalibrationStudy.CANDIDATES.get(raid).get(1),
                entry.get("selectedCorrection"));
        }
        final var timeout = SCREEN.stream().filter(sample -> sample.cell().encounter().equals(RaidType.WOLFPACK.name())
            && sample.correction() == 0.9).findFirst().orElseThrow();
        summaries.set(timeout.caseId() - 1, Map.of("outcomes", Map.of("WIN", 10, "TIMEOUT", 1)));
        final var failed = Step172RaidCalibrationStudy.selection(SCREEN, summaries);
        Assertions.assertEquals(false, failed.get("passesPredeclaredRule"));
        final var wolf = (Map<?, ?>) ((Map<?, ?>) failed.get("raids")).get(RaidType.WOLFPACK.name());
        Assertions.assertEquals(0.9, wolf.get("selectedCorrection"));
    }

    @Test
    void selectionBreaksExactTiesTowardLargerCorrectionAndRejectsAnOutOfBandScreen() {
        final var tied = SCREEN.stream().map(unused -> outcomes(10)).toList();
        final var raids = (Map<?, ?>) Step172RaidCalibrationStudy.selection(SCREEN, tied).get("raids");
        for (final var raid : Step172RaidCalibrationStudy.RAIDS) {
            final var entry = (Map<?, ?>) raids.get(raid.name());
            Assertions.assertEquals(Step172RaidCalibrationStudy.CANDIDATES.get(raid).stream()
                .mapToDouble(Double::doubleValue).max().orElseThrow(), entry.get("selectedCorrection"));
        }
        Assertions.assertEquals(false, Step172RaidCalibrationStudy.selection(SCREEN,
            SCREEN.stream().map(unused -> outcomes(0)).toList()).get("passesPredeclaredRule"));
    }

    @Test
    void timeoutInAnUnselectedScreenCandidateStillRejectsTheWholeScreen() {
        final var summaries = new ArrayList<Map<String, Object>>();
        for (final var sample : SCREEN) {
            final var grid = Step172RaidCalibrationStudy.CANDIDATES.get(RaidType.valueOf(sample.cell().encounter()));
            summaries.add(outcomes(grid.indexOf(sample.correction()) == 1 ? 10 : 0));
        }
        Assertions.assertEquals(true,
            Step172RaidCalibrationStudy.selection(SCREEN, summaries).get("passesPredeclaredRule"));
        summaries.set(0, Map.of("outcomes", Map.of("TIMEOUT", 1)));
        final var result = Step172RaidCalibrationStudy.selection(SCREEN, summaries);
        Assertions.assertEquals(false, result.get("passesPredeclaredRule"));
        final var wolf = (Map<?, ?>) ((Map<?, ?>) result.get("raids")).get(RaidType.WOLFPACK.name());
        Assertions.assertEquals(0.9, wolf.get("selectedCorrection"));
        Assertions.assertEquals(true, wolf.get("passesPredeclaredRule"));
    }

    @Test
    void holdoutRequiresEveryRaidNearHalfButDoesNotRequireEveryTierOrDiagnosticToBeHalf() {
        final var summaries = new ArrayList<Map<String, Object>>();
        for (final var sample : FINAL) {
            final int wins = sample.family().equals("A_MAIN") ? switch (sample.cell().tier()) {
                case BEGINNER -> 0;
                case MID -> 50;
                case STRONG -> 100;
                default -> throw new IllegalStateException("Unexpected calibration tier");
            } : 0;
            summaries.add(outcomes(wins));
        }
        final var result = Step172RaidCalibrationStudy.finalAcceptance(FINAL, summaries);
        Assertions.assertEquals(true, result.get("passesPredeclaredRule"));
        for (final var value : ((Map<?, ?>) result.get("raids")).values()) {
            final var assessment = (Map<?, ?>) value;
            Assertions.assertEquals(33, assessment.get("mainContextCount"));
            Assertions.assertEquals(0.5, assessment.get("balancedMainWinFraction"));
            Assertions.assertEquals(3300, assessment.get("rawMainBattlesNotAcceptanceMetric"));
        }
        final var diagnostic = FINAL.stream().filter(sample -> sample.family().equals("H_LEVELS")).findFirst().orElseThrow();
        summaries.set(diagnostic.caseId() - 1, Map.of("outcomes", Map.of("TIMEOUT", 1)));
        Assertions.assertEquals(false,
            Step172RaidCalibrationStudy.finalAcceptance(FINAL, summaries).get("passesPredeclaredRule"));
    }

    @Test
    void userApprovedWolfBandAcceptsFiftySixPercentButOtherRaidBandsDoNot() {
        final var summaries = new ArrayList<Map<String, Object>>();
        for (final var sample : FINAL) {
            final int wins = sample.family().equals("A_MAIN")
                ? sample.cell().encounter().equals(RaidType.WOLFPACK.name()) ? 56 : 50 : 0;
            summaries.add(outcomes(wins));
        }
        final var approved = Step172RaidCalibrationStudy.finalAcceptance(FINAL, summaries);
        Assertions.assertEquals(true, approved.get("passesPredeclaredRule"));
        final var wolf = (Map<?, ?>) ((Map<?, ?>) approved.get("raids")).get(RaidType.WOLFPACK.name());
        Assertions.assertEquals(0.56, (Double) wolf.get("balancedMainWinFraction"), 1e-12);
        Assertions.assertEquals(true, wolf.get("passesPredeclaredRule"));
        for (final var sample : FINAL) {
            if (sample.family().equals("A_MAIN") && sample.cell().encounter().equals(RaidType.ZOMBIE_HORDE.name())) {
                summaries.set(sample.caseId() - 1, outcomes(56));
            }
        }
        final var rejected = Step172RaidCalibrationStudy.finalAcceptance(FINAL, summaries);
        Assertions.assertEquals(false, rejected.get("passesPredeclaredRule"));
        final var zombie = (Map<?, ?>) ((Map<?, ?>) rejected.get("raids")).get(RaidType.ZOMBIE_HORDE.name());
        Assertions.assertEquals(false, zombie.get("passesPredeclaredRule"));
    }

    private static Map<String, Object> outcomes(int wins) {
        return Map.of("outcomes", Map.of("WIN", wins));
    }
}
