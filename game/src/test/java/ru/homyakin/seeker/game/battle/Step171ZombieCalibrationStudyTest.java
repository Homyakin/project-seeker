package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step171ZombieCalibrationStudy.Phase;
import ru.homyakin.seeker.game.battle.Step171ZombieCalibrationStudy.Sample;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.EquipmentPowerCalculator;

/** Fixed matrices and decision arithmetic only: no generation, battle, application or database startup. */
class Step171ZombieCalibrationStudyTest {
    private static final List<Sample> SCREEN = Step171ZombieCalibrationStudy.cases(Phase.SCREEN);
    private static final List<Sample> FINAL = Step171ZombieCalibrationStudy.cases(Phase.FINAL);

    @Test
    void screenHasTheFixedSeparateMainAndDiagnosticConditionsAndSharedInputSeeds() {
        Assertions.assertEquals(156, SCREEN.size());
        Assertions.assertEquals(3120, Step171ZombieCalibrationStudy.battleCount(SCREEN));
        Assertions.assertEquals(Map.of("S_MAIN", 108L, "S_DIAGNOSTIC", 48L), SCREEN.stream()
            .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
        final var groups = SCREEN.stream().collect(Collectors.groupingBy(Sample::seedGroup));
        Assertions.assertEquals(39, groups.size());
        for (final var group : groups.values()) {
            Assertions.assertEquals(Step171ZombieCalibrationStudy.CANDIDATES,
                group.stream().map(Sample::correction).toList());
            for (final var sample : group) {
                Assertions.assertEquals(group.getFirst().members(), sample.members());
                Assertions.assertEquals(group.getFirst().cell(), sample.cell());
                Assertions.assertEquals(10, sample.cell().raidLevel());
                Assertions.assertEquals(10, sample.pairs());
                Assertions.assertEquals(Step171ZombieCalibrationStudy.generationSeed(group.getFirst(), 0),
                    Step171ZombieCalibrationStudy.generationSeed(sample, 0));
                Assertions.assertEquals(Step171ZombieCalibrationStudy.battleSeed(group.getFirst(), 0),
                    Step171ZombieCalibrationStudy.battleSeed(sample, 0));
            }
        }
    }

    @Test
    void holdoutHasTheOldConditionsIntermediateLevelsAndOnlySixOversampledCells() {
        Assertions.assertEquals(401, FINAL.size());
        Assertions.assertEquals(8300, Step171ZombieCalibrationStudy.battleCount(FINAL));
        Assertions.assertEquals(Map.of("A_MAIN", 120L, "B_TYPES", 108L, "D_EFFECTS", 12L,
            "E_CAUSAL", 72L, "F_DEFAULTS", 20L, "G_WORKABLE", 24L, "H_INTERMEDIATE", 45L), FINAL.stream()
                .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
        Assertions.assertEquals(329, FINAL.stream().map(Sample::seedGroup).distinct().count());
        Assertions.assertEquals(6, FINAL.stream().filter(sample -> sample.pairs() == 100).count());
        for (final var sample : FINAL) {
            Assertions.assertEquals(sample.productionCorrection(), sample.correction());
            if (sample.pairs() == 100) {
                Assertions.assertEquals("A_MAIN", sample.family());
                Assertions.assertEquals(10, sample.cell().raidLevel());
                Assertions.assertTrue(List.of(3, 13).contains(sample.cell().size()));
            }
            if (sample.family().equals("H_INTERMEDIATE")) {
                Assertions.assertTrue(List.of(5, 15, 20).contains(sample.cell().raidLevel()));
                Assertions.assertTrue(List.of(1, 3, 7, 13, 30).contains(sample.cell().size()));
                Assertions.assertEquals(10, sample.pairs());
            }
        }
    }

    @Test
    void phasesUseDisjointFreshRootsAndFinalReplayIncludesTheCriticalLevelTenCells() {
        Assertions.assertEquals(20_261_120_001_000L, Step171ZombieCalibrationStudy.generationSeed(SCREEN.getFirst(), 0));
        Assertions.assertEquals(30_261_120_001_000L, Step171ZombieCalibrationStudy.battleSeed(SCREEN.getFirst(), 0));
        Assertions.assertEquals(20_261_121_001_000L, Step171ZombieCalibrationStudy.generationSeed(FINAL.getFirst(), 0));
        Assertions.assertEquals(30_261_121_001_000L, Step171ZombieCalibrationStudy.battleSeed(FINAL.getFirst(), 0));
        Assertions.assertTrue(Step171ZombieCalibrationStudy.replayCaseIds(SCREEN).isEmpty());
        final var replay = Step171ZombieCalibrationStudy.replayCaseIds(FINAL);
        Assertions.assertEquals(16, replay.size());
        Assertions.assertEquals(16, replay.stream().distinct().count());
        Assertions.assertEquals(List.of(3, 7, 13, 30), replay.stream().map(id -> FINAL.get(id - 1))
            .filter(sample -> sample.family().equals("A_MAIN") && sample.cell().raidLevel() == 10)
            .map(sample -> sample.cell().size()).toList());
    }

    @Test
    void screeningOverrideIsExplicitAndFinalAlwaysUsesTheActualLevelBonus() {
        for (final var sample : SCREEN) {
            Assertions.assertEquals(Step171ZombieCalibrationStudy.baseLevelBonus(sample) * sample.correction()
                / sample.productionCorrection(), Step171ZombieCalibrationStudy.requestedPowerBonus(sample));
        }
        for (final var sample : FINAL) {
            Assertions.assertEquals(Step171ZombieCalibrationStudy.baseLevelBonus(sample),
                Step171ZombieCalibrationStudy.requestedPowerBonus(sample));
        }
    }

    @Test
    void actualInputsHaveNoOverlappingSlotsAndTheBluntDiagnosticIsExplicitlyCommonLeather() {
        for (final var sample : java.util.stream.Stream.concat(SCREEN.stream(), FINAL.stream()).toList()) {
            for (final var member : sample.members()) {
                final var slots = EnumSet.noneOf(PersonageSlot.class);
                for (final var item : member.items()) {
                    item.object().slots().forEach(slot -> Assertions.assertTrue(slots.add(slot)));
                }
                Assertions.assertEquals(EnumSet.allOf(PersonageSlot.class), slots);
                final var power = EquipmentPowerCalculator.calculate(member.items()).unscaled();
                Assertions.assertTrue(Double.isFinite(power) && power > 0);
                if (sample.cell().style().equals("BLUNT_LEATHER")) {
                    Assertions.assertTrue(member.items().stream().allMatch(item -> item.modifier().isEmpty()));
                    Assertions.assertEquals(List.of(AttackType.BLUNT), member.items().stream()
                        .flatMap(item -> item.itemAttacks().stream()).map(attack -> attack.attackType()).distinct().toList());
                    Assertions.assertEquals(List.of(DefenseType.LEATHER), member.items().stream()
                        .flatMap(item -> item.itemDefense().stream()).map(defense -> defense.defenseType()).distinct().toList());
                }
            }
        }
    }

    @Test
    void selectionUsesOnlyTheDeclaredMainContextsAndChoosesTheLargestPassingCorrection() {
        final var summaries = new ArrayList<Map<String, Object>>();
        for (final var sample : SCREEN) {
            final int wins = sample.family().equals("S_MAIN") ? sample.correction() == 1 ? 2 : 5 : 20;
            summaries.add(Map.of("outcomes", Map.of("WIN", wins, "LOSS", 20 - wins)));
        }
        final var result = Step171ZombieCalibrationStudy.selection(SCREEN, summaries);
        Assertions.assertEquals(0.8, result.get("selectedCorrection"));
        final var diagnostic = SCREEN.stream().filter(sample -> sample.family().equals("S_DIAGNOSTIC")
            && sample.correction() == 0.8).findFirst().orElseThrow();
        summaries.set(diagnostic.caseId() - 1, Map.of("outcomes", Map.of("TIMEOUT", 1, "WIN", 19)));
        Assertions.assertEquals(0.65, Step171ZombieCalibrationStudy.selection(SCREEN, summaries).get("selectedCorrection"));
    }

    @Test
    void holdoutAcceptanceDoesNotMistakeOversampledRawVictoriesForBalancedContextSuccess() {
        final var summaries = new ArrayList<Map<String, Object>>();
        for (final var sample : FINAL) {
            summaries.add(Map.of("outcomes", Map.of("WIN", sample.pairs() == 100 ? 200 : 0)));
        }
        final var acceptance = Step171ZombieCalibrationStudy.finalAcceptance(FINAL, summaries);
        Assertions.assertEquals(27, acceptance.get("mainContextCount"));
        Assertions.assertEquals(1200, acceptance.get("rawMainWinsNotAcceptanceMetric"));
        Assertions.assertEquals(1620, acceptance.get("rawMainBattlesNotAcceptanceMetric"));
        Assertions.assertEquals(2.0 / 9, (Double) acceptance.get("balancedMainWinFraction"), 1e-15);
        Assertions.assertEquals(false, acceptance.get("passesPredeclaredRule"));
    }
}
