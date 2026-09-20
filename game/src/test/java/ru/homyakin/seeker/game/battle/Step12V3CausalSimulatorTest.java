package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step12CausalStatistics.ConfidenceInterval;
import ru.homyakin.seeker.game.battle.Step12CausalStatistics.Estimate;
import ru.homyakin.seeker.game.battle.Step12V3CausalObservationRunner.TempoBenchmarkObservation;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12V3AcceptanceLedger;

class Step12V3CausalSimulatorTest {
    @Test
    void historicalV2BothDisabledFactorialWorldKeepsLegacyBattleRules() {
        final var roles = Set.of(V2Build.SKIRMISHER_SLASH, V2Build.BREAKER_CLOSE_SLASH);
        final var teams = Step12SimulationFixtures.v2Teams(V2Matchup.SKIRMISHER_V2, 0, roles);

        Assertions.assertTrue(teams.evaluatedTeam().stream().noneMatch(BattlePersonage::hasScalingSkills));
        Assertions.assertTrue(teams.opponents().stream().noneMatch(BattlePersonage::hasScalingSkills));
        Assertions.assertTrue(teams.evaluatedTeam().stream().noneMatch(BattlePersonage::usesScalingCombatRules));
        Assertions.assertTrue(teams.opponents().stream().noneMatch(BattlePersonage::usesScalingCombatRules));
        Assertions.assertFalse(new BattleContext(teams.evaluatedTeam(), teams.opponents()).usesScalingSkillOrder());
    }

    @Test
    void bothDisabledFactorialWorldKeepsVersionedBattleRules() {
        final var matchup = Step12SimulationFixtures.V3Matchup.SKIRMISHER_V3;
        final var roles = Set.of(
            Step12SimulationFixtures.V3Build.SKIRMISHER_SLASH,
            Step12SimulationFixtures.V3Build.BREAKER_CLOSE_SLASH
        );
        final var teams = Step12SimulationFixtures.v3Teams(matchup, 0, roles);

        Assertions.assertTrue(teams.evaluatedTeam().stream().noneMatch(BattlePersonage::hasScalingSkills));
        Assertions.assertTrue(teams.opponents().stream().noneMatch(BattlePersonage::hasScalingSkills));
        Assertions.assertTrue(teams.evaluatedTeam().stream().allMatch(BattlePersonage::usesScalingCombatRules));
        Assertions.assertTrue(teams.opponents().stream().allMatch(BattlePersonage::usesScalingCombatRules));
        Assertions.assertTrue(new BattleContext(teams.evaluatedTeam(), teams.opponents()).usesScalingSkillOrder());
    }

    @Test
    void configurationSeparatesDiagnosticAndCausalPreflightRoots() {
        final var accepted = configuration("DIAGNOSTIC", Step12V3CausalSimulator.DIAGNOSTIC_ROOT);
        final var preflight = preflightConfiguration(Step12V3CausalSimulator.CAUSAL_PREFLIGHT_ROOT);
        final var finalRun = finalConfiguration(Step12V3CausalSimulator.FINAL_ROOT);

        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(
                () -> Step12V3CausalSimulator.validateConfiguration(accepted)
            ),
            () -> Assertions.assertDoesNotThrow(
                () -> Step12V3CausalSimulator.validateConfiguration(preflight)
            ),
            () -> Assertions.assertDoesNotThrow(
                () -> Step12V3CausalSimulator.validateConfiguration(finalRun)
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.validateConfiguration(
                    configuration("DIAGNOSTIC", Step12V3CausalSimulator.FINAL_ROOT)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.validateConfiguration(
                    configuration("FINAL", Step12V3CausalSimulator.DIAGNOSTIC_ROOT)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.validateConfiguration(
                    preflightConfiguration(Step12V3CausalSimulator.OUTCOME_PREFLIGHT_ROOT)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.validateConfiguration(
                    preflightConfiguration(Step12V3CausalSimulator.DIAGNOSTIC_ROOT)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.validateConfiguration(
                    configuration("PREFLIGHT", Step12V3CausalSimulator.CAUSAL_PREFLIGHT_ROOT)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.validateConfiguration(configuration("DIAGNOSTIC", 1L))
            )
        );
    }

    @Test
    void smallRunUsesOnlyV3FixturesCodesAndReportHeading() {
        final var run = Step12V3CausalSimulator.run(
            configuration("DIAGNOSTIC", Step12V3CausalSimulator.DIAGNOSTIC_ROOT),
            List.of(V3Matchup.ASSASSIN_V3),
            List.of(0),
            List.of()
        );

        final var result = Assertions.assertDoesNotThrow(() -> run.results().getFirst());
        Assertions.assertAll(
            () -> Assertions.assertEquals(V3Matchup.ASSASSIN_V3, result.cell().matchup()),
            () -> Assertions.assertEquals("БЛАГО-УБИЙЦА_V3_ПРИЧИНА_L0", result.cell().code()),
            () -> Assertions.assertTrue(result.primary().accepted()),
            () -> Assertions.assertTrue(run.guardianResults().isEmpty()),
            () -> Assertions.assertTrue(run.markdown().startsWith(
                "# Причинная диагностика третьей редакции шага 12"
            )),
            () -> Assertions.assertTrue(run.markdown().contains("`V3_CAUSAL_1`")),
            () -> Assertions.assertTrue(run.markdown().contains("`2026092101`")),
            () -> Assertions.assertTrue(run.markdown().contains(result.cell().code())),
            () -> Assertions.assertFalse(run.markdown().contains("_V2_"))
        );
    }

    @Test
    void guardianGateUsesFirstOpportunityTurnsAndCapButNotLifetimeDamage() {
        final var useful = estimate(0.80, 0.80, 0.81);
        final var positiveTurns = estimate(1.0, 0.1, 1.9);

        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12V3CausalSimulator.guardianAccepted(
                useful,
                positiveTurns,
                0.05,
                true,
                "DIAGNOSTIC"
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.guardianAccepted(
                estimate(0.75, 0.74, 0.75),
                positiveTurns,
                0.05,
                true,
                "DIAGNOSTIC"
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.guardianAccepted(
                useful,
                estimate(0.0, 0.0, 0.1),
                0.05,
                true,
                "DIAGNOSTIC"
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.guardianAccepted(
                useful,
                positiveTurns,
                0.050_001,
                true,
                "DIAGNOSTIC"
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.guardianAccepted(
                useful,
                positiveTurns,
                0.05,
                false,
                "DIAGNOSTIC"
            ))
        );
    }

    @Test
    void preflightUsesPointEstimateForBinaryGateWhileDiagnosticKeepsFinalPrecisionGate() {
        final var estimate = estimate(0.80, 0.76, 0.84);

        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12V3CausalSimulator.thresholdAccepted(
                estimate,
                "PREFLIGHT"
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.thresholdAccepted(
                estimate,
                "DIAGNOSTIC"
            ))
        );
    }

    @Test
    void preflightRejectsUnsafeExecutionSettings() {
        final long root = Step12V3CausalSimulator.CAUSAL_PREFLIGHT_ROOT;

        Assertions.assertAll(
            () -> assertInvalid(preflightConfiguration(root, 1, 10_000, 1, true, "target/report.md")),
            () -> assertInvalid(preflightConfiguration(root, 2_000, 9_999, 1, true, "target/report.md")),
            () -> assertInvalid(preflightConfiguration(root, 2_000, 10_000, 0, true, "target/report.md")),
            () -> assertInvalid(preflightConfiguration(root, 2_000, 10_000, 1, false, "target/report.md")),
            () -> assertInvalid(preflightConfiguration(root, 2_000, 10_000, 1, true, " "))
        );
    }

    @Test
    void preflightRequiresCompleteSelectionBeforeStartingAnyBattle() {
        final var valid = preflightConfiguration(Step12V3CausalSimulator.CAUSAL_PREFLIGHT_ROOT);
        final var allMatchups = List.of(V3Matchup.values());
        final var allLevels = Step12SimulationFixtures.CONTROL_LEVELS;

        Assertions.assertAll(
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.run(
                    valid,
                    List.of(V3Matchup.ASSASSIN_V3),
                    allLevels,
                    List.of(3, 7)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.run(
                    valid,
                    allMatchups,
                    List.of(0),
                    List.of(3, 7)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.run(
                    valid,
                    allMatchups,
                    allLevels,
                    List.of(3)
                )
            )
        );
    }

    @Test
    void finalRequiresExactSettingsAndCompleteSelectionBeforeStartingAnyBattle() {
        final var valid = finalConfiguration(Step12V3CausalSimulator.FINAL_ROOT);
        final var allMatchups = List.of(V3Matchup.values());
        final var allLevels = Step12SimulationFixtures.CONTROL_LEVELS;

        Assertions.assertAll(
            () -> assertInvalid(new Step12V3CausalSimulator.Configuration(
                "FINAL", Step12V3CausalSimulator.FINAL_ROOT, 9_999, 10_000, 1, true, "ignored"
            )),
            () -> assertInvalid(new Step12V3CausalSimulator.Configuration(
                "FINAL", Step12V3CausalSimulator.FINAL_ROOT, 10_000, 9_999, 1, true, "ignored"
            )),
            () -> assertInvalid(new Step12V3CausalSimulator.Configuration(
                "FINAL", Step12V3CausalSimulator.FINAL_ROOT, 10_000, 10_000, 1, false, "ignored"
            )),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12V3CausalSimulator.run(
                    valid,
                    allMatchups,
                    List.of(0),
                    List.of(3, 7)
                )
            ),
            () -> Assertions.assertThrows(
                IllegalStateException.class,
                () -> Step12V3CausalSimulator.run(
                    valid,
                    allMatchups,
                    allLevels,
                    List.of(3, 7)
                )
            )
        );
    }

    @Test
    void acceptanceFingerprintCoversEveryFactorialAndGuardianCell() {
        final var preflightDescriptors = Step12V3CausalSimulator.acceptanceCellDescriptors(
            Step12V3CausalSimulator.CAUSAL_PREFLIGHT_ROOT,
            List.of(V3Matchup.values()),
            Step12SimulationFixtures.CONTROL_LEVELS,
            List.of(3, 7)
        );
        final var finalDescriptors = Step12V3CausalSimulator.acceptanceCellDescriptors(
            Step12V3CausalSimulator.FINAL_ROOT,
            List.of(V3Matchup.values()),
            Step12SimulationFixtures.CONTROL_LEVELS,
            List.of(3, 7)
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(130, finalDescriptors.size()),
            () -> Assertions.assertEquals(130, finalDescriptors.stream().distinct().count()),
            () -> Assertions.assertEquals(120, finalDescriptors.stream()
                .filter(descriptor -> descriptor.startsWith("FACTORIAL|"))
                .count()),
            () -> Assertions.assertEquals(10, finalDescriptors.stream()
                .filter(descriptor -> descriptor.startsWith("GUARDIAN|"))
                .count()),
            () -> Assertions.assertEquals(
                Step12V3AcceptanceLedger.RunKind.CAUSAL_PREFLIGHT.expectedCellSetFingerprint(),
                Step12V3AcceptanceLedger.cellSetFingerprint(preflightDescriptors)
            ),
            () -> Assertions.assertEquals(
                Step12V3AcceptanceLedger.RunKind.CAUSAL_FINAL.expectedCellSetFingerprint(),
                Step12V3AcceptanceLedger.cellSetFingerprint(finalDescriptors)
            )
        );
    }

    @Test
    void tempoGateRequiresExactLongControlAndLossInsideOpenClosedInterval() {
        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12V3CausalSimulator.tempoBenchmarkAccepted(
                new TempoBenchmarkObservation(27_200, 32_000, 0.15)
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.tempoBenchmarkAccepted(
                new TempoBenchmarkObservation(32_000, 32_000, 0.0)
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.tempoBenchmarkAccepted(
                new TempoBenchmarkObservation(27_199, 32_000, 0.150_031_25)
            )),
            () -> Assertions.assertFalse(Step12V3CausalSimulator.tempoBenchmarkAccepted(
                new TempoBenchmarkObservation(27_200, 31_999, 0.149_973_44)
            ))
        );
    }

    @Test
    void tempoExactObservationUsesImpactNinetyTwoAndRemovesThreeHundredThirtyOne() {
        final var observation = Step12V3CausalObservationRunner.tempoExact(
            0,
            Step12V3CausalSimulator.DIAGNOSTIC_ROOT
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(92, observation.firstImpactStrength()),
            () -> Assertions.assertEquals(92, observation.secondImpactStrength()),
            () -> Assertions.assertEquals(331, observation.firstRemoved()),
            () -> Assertions.assertEquals(331, observation.secondRemoved()),
            () -> Assertions.assertTrue(observation.accepted())
        );
    }

    private static Step12V3CausalSimulator.Configuration configuration(String mode, long seed) {
        return new Step12V3CausalSimulator.Configuration(
            mode,
            seed,
            1,
            10_000,
            1,
            false,
            "target/ignored-v3-causal-test.md"
        );
    }

    private static Step12V3CausalSimulator.Configuration preflightConfiguration(long seed) {
        return preflightConfiguration(
            seed,
            2_000,
            10_000,
            1,
            true,
            "target/ignored-v3-causal-test.md"
        );
    }

    private static Step12V3CausalSimulator.Configuration finalConfiguration(long seed) {
        return new Step12V3CausalSimulator.Configuration(
            "FINAL",
            seed,
            10_000,
            10_000,
            1,
            true,
            "documentation/raid-redesign-step-12-v3-causal-final.md"
        );
    }

    private static Step12V3CausalSimulator.Configuration preflightConfiguration(
        long seed,
        int iterations,
        int maxRounds,
        int workers,
        boolean enforce,
        String output
    ) {
        return new Step12V3CausalSimulator.Configuration(
            "PREFLIGHT",
            seed,
            iterations,
            maxRounds,
            workers,
            enforce,
            output
        );
    }

    private static void assertInvalid(Step12V3CausalSimulator.Configuration configuration) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12V3CausalSimulator.validateConfiguration(configuration)
        );
    }

    private static Estimate estimate(double value, double lower, double upper) {
        return new Estimate(value, new ConfidenceInterval(lower, upper));
    }
}
