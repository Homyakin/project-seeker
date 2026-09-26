package ru.homyakin.seeker.game.battle.simulation;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.ConfidenceInterval;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator.PairOutcome;
import ru.homyakin.seeker.game.battle.simulation.Step12CombatMatrixSimulator.MatrixFamily;
import ru.homyakin.seeker.game.battle.simulation.Step12CombatMatrixSimulator.MatrixRunConfiguration;
import ru.homyakin.seeker.game.battle.simulation.Step12CombatMatrixSimulator.MirrorFixture;
import ru.homyakin.seeker.game.battle.simulation.Step12CombatMatrixSimulator.V2FavorableFixture;
import ru.homyakin.seeker.game.battle.simulation.Step12CombatMatrixSimulator.V3FavorableFixture;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Matchup;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;

class Step12CombatMatrixTest {
    private static final Map<RoleBuild, ActiveEnum> EXPECTED_SKILLS = Map.ofEntries(
        Map.entry(RoleBuild.GUARDIAN, ActiveEnum.GUARD),
        Map.entry(RoleBuild.BRUISER, ActiveEnum.BERSERK),
        Map.entry(RoleBuild.SKIRMISHER, ActiveEnum.HIT_AND_RUN),
        Map.entry(RoleBuild.ASSASSIN, ActiveEnum.PENETRATION),
        Map.entry(RoleBuild.RANGER_PHYSICAL, ActiveEnum.DOUBLE_ATTACK),
        Map.entry(RoleBuild.RANGER_MAGICAL, ActiveEnum.DOUBLE_ATTACK),
        Map.entry(RoleBuild.BREAKER_PHYSICAL, ActiveEnum.ACCUMULATION),
        Map.entry(RoleBuild.BREAKER_MAGICAL, ActiveEnum.ACCUMULATION),
        Map.entry(RoleBuild.TACTICIAN_PHYSICAL, ActiveEnum.TEMPO_BREAK),
        Map.entry(RoleBuild.TACTICIAN_MAGICAL, ActiveEnum.TEMPO_BREAK)
    );

    @Test
    void mainBuildsUseExactScalingCatalogEquipmentAndFourPointModifiers() {
        Assertions.assertEquals(10, RoleBuild.values().length);

        for (final var build : RoleBuild.values()) {
            final var items = Step12SimulationFixtures.items(build, 10);
            final var modified = items.stream().filter(item -> item.modifier().isPresent()).toList();
            final var personage = Step12SimulationFixtures.personage(build, 10);

            Assertions.assertAll(
                build.name(),
                () -> Assertions.assertTrue(items.stream().allMatch(item -> item.enhanceLevel() == 10)),
                () -> Assertions.assertTrue(items.stream().allMatch(item ->
                    item.object().progressionVersion() == ItemProgressionVersion.V1
                )),
                () -> Assertions.assertEquals(1, modified.size()),
                () -> Assertions.assertEquals(4, modified.getFirst().skillPoints()),
                () -> Assertions.assertEquals(build.position(), personage.startPosition()),
                () -> Assertions.assertEquals(build.targetingTactic(), personage.targetingTactic()),
                () -> Assertions.assertEquals(build.displayName(), personage.name().orElseThrow()),
                () -> Assertions.assertEquals(1, personage.skillSnapshots().size()),
                () -> Assertions.assertEquals(EXPECTED_SKILLS.get(build), personage.skillSnapshots().getFirst().code()),
                () -> Assertions.assertEquals(4, personage.skillSnapshots().getFirst().points()),
                () -> Assertions.assertEquals(
                    SkillFormulaVersion.SCALING_SKILLS_V2,
                    personage.skillSnapshots().getFirst().formulaVersion()
                )
            );
        }
    }

    @Test
    void levelZeroBuildsMatchPreregisteredCharacteristics() {
        final Map<RoleBuild, ExpectedStats> expected = new EnumMap<>(RoleBuild.class);
        expected.put(RoleBuild.GUARDIAN, stats(
            2_700, DefenseType.PLATE, 660, AttackType.BLUNT, 360, 0, 0, 0, 2, 0, 1.25, 86, 50, 80
        ));
        expected.put(RoleBuild.BRUISER, stats(
            1_920, DefenseType.LEATHER, 480, AttackType.BLUNT, 480, 0, 0, 0, 8, 12, 1.38, 130, 18, 85
        ));
        expected.put(RoleBuild.SKIRMISHER, stats(
            1_380, DefenseType.CLOTH, 360, AttackType.PIERCE, 360, 240, 0, 0, 9, 15, 1.39, 184, 7, 91
        ));
        expected.put(RoleBuild.ASSASSIN, stats(
            1_620, DefenseType.ARCANE, 480, AttackType.PIERCE, 480, 360, 0, 0, 19, 8, 1.65, 154, 4, 95
        ));
        expected.put(RoleBuild.RANGER_PHYSICAL, stats(
            1_380, DefenseType.CLOTH, 360, AttackType.PIERCE, 240, 240, 360, 360, 8, 11, 1.39, 156, 4, 87
        ));
        expected.put(RoleBuild.RANGER_MAGICAL, stats(
            1_380, DefenseType.CLOTH, 360, AttackType.MAGICAL, 240, 240, 360, 360, 8, 11, 1.39, 156, 4, 87
        ));
        expected.put(RoleBuild.BREAKER_PHYSICAL, stats(
            1_620, DefenseType.ARCANE, 480, AttackType.PIERCE, 360, 360, 360, 0, 14, 6, 1.60, 144, 3, 100
        ));
        expected.put(RoleBuild.BREAKER_MAGICAL, stats(
            1_620, DefenseType.ARCANE, 480, AttackType.MAGICAL, 360, 360, 360, 0, 14, 6, 1.60, 144, 3, 100
        ));
        expected.put(RoleBuild.TACTICIAN_PHYSICAL, stats(
            1_620, DefenseType.ARCANE, 480, AttackType.PIERCE, 360, 360, 0, 0, 14, 6, 1.60, 144, 3, 100
        ));
        expected.put(RoleBuild.TACTICIAN_MAGICAL, stats(
            1_620, DefenseType.ARCANE, 480, AttackType.MAGICAL, 360, 360, 0, 0, 14, 6, 1.60, 144, 3, 100
        ));

        for (final var entry : expected.entrySet()) {
            assertStats(entry.getKey(), Step12SimulationFixtures.personage(entry.getKey(), 0), entry.getValue());
        }
    }

    @Test
    void roleTeamsHaveStablePreregisteredComposition() {
        final var one = Step12SimulationFixtures.roleTeam(RoleBuild.ASSASSIN, 1, 3);
        final var three = Step12SimulationFixtures.roleTeam(RoleBuild.ASSASSIN, 3, 3);
        final var seven = Step12SimulationFixtures.roleTeam(RoleBuild.ASSASSIN, 7, 3);

        Assertions.assertEquals(List.of("Убийца"), names(one));
        Assertions.assertEquals(List.of("Опора-фронт", "Убийца", "Опора-тыл"), names(three));
        Assertions.assertEquals(
            List.of("Опора-фронт", "Опора-фронт", "Убийца", "Убийца", "Убийца", "Опора-тыл", "Опора-тыл"),
            names(seven)
        );
        Assertions.assertEquals(List.of(Position.FRONT, Position.FRONT, Position.BACK), positions(three));
        Assertions.assertTrue(three.getFirst().skillSnapshots().isEmpty());
        Assertions.assertTrue(three.getLast().skillSnapshots().isEmpty());
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12SimulationFixtures.roleTeam(RoleBuild.ASSASSIN, 2, 3)
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12SimulationFixtures.roleTeam(RoleBuild.ASSASSIN, 3, 4)
        );
    }

    @Test
    void matrixGeneratesAllMirrorAndFavorableCellsWithStableDistinctSeeds() {
        final var first = Step12CombatMatrixSimulator.cells(20_260_912L, "V2");
        final var second = Step12CombatMatrixSimulator.cells(20_260_912L, "V2");
        final var favorable = first.stream()
            .filter(it -> it.family() == MatrixFamily.FAVORABLE)
            .toList();

        Assertions.assertAll(
            () -> Assertions.assertEquals(first, second),
            () -> Assertions.assertEquals(180, first.size()),
            () -> Assertions.assertEquals(150, countFamily(first, MatrixFamily.MIRROR)),
            () -> Assertions.assertEquals(30, countFamily(first, MatrixFamily.FAVORABLE)),
            () -> Assertions.assertEquals(180, first.stream().map(it -> it.code()).distinct().count()),
            () -> Assertions.assertEquals(180, first.stream().map(it -> it.seed()).distinct().count()),
            () -> Assertions.assertTrue(first.stream()
                .filter(it -> it.family() == MatrixFamily.MIRROR)
                .allMatch(it -> it.fixture() instanceof MirrorFixture)),
            () -> Assertions.assertTrue(favorable.stream().allMatch(it -> it.partySize() == 3)),
            () -> Assertions.assertTrue(favorable.stream()
                .allMatch(it -> it.fixture() instanceof V2FavorableFixture)),
            () -> Assertions.assertTrue(favorable.stream()
                .allMatch(it -> it.code().contains("_V2_N3_L"))),
            () -> Assertions.assertTrue(favorable.stream().allMatch(it -> it.causalGate().separateRequired())),
            () -> Assertions.assertTrue(Arrays.stream(V2Matchup.values()).allMatch(matchup ->
                favorable.stream()
                    .filter(it -> ((V2FavorableFixture) it.fixture()).matchup() == matchup)
                    .map(it -> it.enhanceLevel())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet())
                    .equals(Set.copyOf(Step12SimulationFixtures.CONTROL_LEVELS))
            )),
            () -> Assertions.assertDoesNotThrow(() -> Step12CombatMatrixSimulator.validateFinalCells(first))
        );
    }

    @Test
    void revisionThreeMatrixUsesEveryRevisionThreeMatchupAtEveryControlLevel() {
        final var cells = Step12CombatMatrixSimulator.cells(2_026_092_201L, "V3");
        final var favorable = cells.stream()
            .filter(it -> it.family() == MatrixFamily.FAVORABLE)
            .toList();

        Assertions.assertAll(
            () -> Assertions.assertEquals(180, cells.size()),
            () -> Assertions.assertEquals(150, countFamily(cells, MatrixFamily.MIRROR)),
            () -> Assertions.assertEquals(30, countFamily(cells, MatrixFamily.FAVORABLE)),
            () -> Assertions.assertEquals(180, cells.stream().map(it -> it.code()).distinct().count()),
            () -> Assertions.assertEquals(180, cells.stream().map(it -> it.seed()).distinct().count()),
            () -> Assertions.assertTrue(favorable.stream()
                .allMatch(it -> it.fixture() instanceof V3FavorableFixture)),
            () -> Assertions.assertTrue(favorable.stream()
                .allMatch(it -> it.code().contains("_V3_N3_L"))),
            () -> Assertions.assertTrue(favorable.stream().allMatch(it -> it.causalGate().separateRequired())),
            () -> Assertions.assertTrue(Arrays.stream(V3Matchup.values()).allMatch(matchup ->
                favorable.stream()
                    .filter(it -> ((V3FavorableFixture) it.fixture()).matchup() == matchup)
                    .map(it -> it.enhanceLevel())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet())
                    .equals(Set.copyOf(Step12SimulationFixtures.CONTROL_LEVELS))
            )),
            () -> Assertions.assertDoesNotThrow(() ->
                Step12CombatMatrixSimulator.validateFinalV3Cells(cells)
            )
        );
    }

    @Test
    void outcomeAcceptanceFingerprintCoversEverySelectedCell() {
        final var preflightDescriptors = Step12CombatMatrixSimulator.acceptanceCellDescriptors(
            Step12CombatMatrixSimulator.cells(2_026_092_201L, "V3")
        );
        final var finalDescriptors = Step12CombatMatrixSimulator.acceptanceCellDescriptors(
            Step12CombatMatrixSimulator.cells(2_026_092_301L, "V3")
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(180, preflightDescriptors.size()),
            () -> Assertions.assertEquals(180, preflightDescriptors.stream().distinct().count()),
            () -> Assertions.assertEquals(
                Step12V3AcceptanceLedger.RunKind.OUTCOME_PREFLIGHT.expectedCellSetFingerprint(),
                Step12V3AcceptanceLedger.cellSetFingerprint(preflightDescriptors)
            ),
            () -> Assertions.assertEquals(
                Step12V3AcceptanceLedger.RunKind.OUTCOME_FINAL.expectedCellSetFingerprint(),
                Step12V3AcceptanceLedger.cellSetFingerprint(finalDescriptors)
            )
        );
    }

    @Test
    void v4OutcomeFingerprintUsesNewRootsAndTheSameV3CandidateCells() {
        final var preflightDescriptors = Step12CombatMatrixSimulator.acceptanceCellDescriptors(
            Step12CombatMatrixSimulator.cells(2_026_092_203L, "V3")
        );
        final var finalDescriptors = Step12CombatMatrixSimulator.acceptanceCellDescriptors(
            Step12CombatMatrixSimulator.cells(2_026_092_302L, "V3")
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(180, preflightDescriptors.size()),
            () -> Assertions.assertTrue(preflightDescriptors.stream()
                .filter(it -> it.contains("|FAVORABLE|"))
                .allMatch(it -> it.contains("_V3_"))),
            () -> Assertions.assertEquals(
                Step12V4AcceptanceLedger.RunKind.OUTCOME_PREFLIGHT.expectedCellSetFingerprint(),
                Step12V4AcceptanceLedger.cellSetFingerprint(preflightDescriptors)
            ),
            () -> Assertions.assertEquals(
                Step12V4AcceptanceLedger.RunKind.OUTCOME_FINAL.expectedCellSetFingerprint(),
                Step12V4AcceptanceLedger.cellSetFingerprint(finalDescriptors)
            )
        );
    }

    @Test
    void mirrorFixturesKeepHistoricalV2AndCurrentV3FormulaVersionsSeparate() {
        final var historical = Step12CombatMatrixSimulator.cells(20_260_912L, "V2").stream()
            .filter(it -> it.code().equals("MIRROR_GUARDIAN_N1_L0"))
            .findFirst()
            .orElseThrow()
            .fixture()
            .teams(1, 0)
            .evaluatedTeam()
            .getFirst();
        final var current = Step12CombatMatrixSimulator.cells(2_026_092_201L, "V3").stream()
            .filter(it -> it.code().equals("MIRROR_GUARDIAN_N1_L0"))
            .findFirst()
            .orElseThrow()
            .fixture()
            .teams(1, 0)
            .evaluatedTeam()
            .getFirst();

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V1,
                historical.skillSnapshots().getFirst().formulaVersion()
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                current.skillSnapshots().getFirst().formulaVersion()
            )
        );
    }

    @Test
    void pairedExecutorIsReproducibleAndAccountsForBothTeamOrders() {
        final var cell = Step12CombatMatrixSimulator.cells(20_260_912L, "V2").stream()
            .filter(it -> it.code().equals("БЛАГО-ГРОМИЛА_V2_N3_L0"))
            .findFirst()
            .orElseThrow();
        final var request = cell.request(4, 10_000);
        final var simulator = new PairedCombatSimulator();

        final var first = simulator.run(request);
        final var second = simulator.run(request);
        final var decision = Step12CombatMatrixSimulator.decision(cell, first);

        Assertions.assertAll(
            () -> Assertions.assertEquals(first, second),
            () -> Assertions.assertEquals(8, first.battles()),
            () -> Assertions.assertEquals(4, first.pairOutcomes().size()),
            () -> Assertions.assertEquals(
                IntStream.range(0, 4).boxed().toList(),
                first.pairOutcomes().stream().map(PairOutcome::index).toList()
            ),
            () -> Assertions.assertEquals(
                first.evaluatedFirst().wins() + 4 - first.opponentsFirst().wins(),
                first.evaluatedWins()
            ),
            () -> Assertions.assertEquals(
                first.evaluatedWins() / 8.0,
                first.evaluatedWinRate()
            ),
            () -> Assertions.assertEquals(
                "ОПОРА + Громила, дробящий + ОПОРА → ОПОРА + Страж, дробящий + ОПОРА",
                first.evaluatedFirst().composition()
            ),
            () -> Assertions.assertEquals(
                "ОПОРА + Страж, дробящий + ОПОРА → ОПОРА + Громила, дробящий + ОПОРА",
                first.opponentsFirst().composition()
            ),
            () -> Assertions.assertEquals(
                (first.evaluatedFirst().wins() + first.opponentsFirst().wins()) / 8.0,
                first.firstSideWinRate()
            ),
            () -> Assertions.assertFalse(decision.outcomeLayerAccepted()),
            () -> Assertions.assertTrue(decision.causalGateDescription().contains("порог «Берсерка»")),
            () -> Assertions.assertTrue(decision.description().contains("отдельные причинные ворота"))
        );
    }

    @Test
    void compactPairedReportPreservesEveryAggregateAndRenderedDetail() {
        final var cell = Step12CombatMatrixSimulator.cells(20_260_912L, "V2").stream()
            .filter(it -> it.code().equals("БЛАГО-ГРОМИЛА_V2_N3_L0"))
            .findFirst()
            .orElseThrow();
        final var raw = new PairedCombatSimulator().run(cell.request(8, 10_000));
        final var compact = raw.withoutRawOutcomes();

        Assertions.assertAll(
            () -> Assertions.assertEquals(8, raw.pairOutcomes().size()),
            () -> Assertions.assertEquals(8, raw.evaluatedFirst().iterationWins().size()),
            () -> Assertions.assertEquals(8, raw.opponentsFirst().iterationWins().size()),
            () -> Assertions.assertTrue(compact.pairOutcomes().isEmpty()),
            () -> Assertions.assertTrue(compact.evaluatedFirst().iterationWins().isEmpty()),
            () -> Assertions.assertTrue(compact.opponentsFirst().iterationWins().isEmpty()),
            () -> Assertions.assertEquals(raw.evaluatedWins(), compact.evaluatedWins()),
            () -> Assertions.assertEquals(raw.evaluatedWinRate(), compact.evaluatedWinRate()),
            () -> Assertions.assertEquals(raw.evaluatedWinRate95(), compact.evaluatedWinRate95()),
            () -> Assertions.assertEquals(raw.firstSideWinRate(), compact.firstSideWinRate()),
            () -> Assertions.assertEquals(raw.bootstrapSeed(), compact.bootstrapSeed()),
            () -> Assertions.assertEquals(
                raw.evaluatedFirst().markdown(),
                compact.evaluatedFirst().markdown()
            ),
            () -> Assertions.assertEquals(
                raw.opponentsFirst().markdown(),
                compact.opponentsFirst().markdown()
            ),
            () -> Assertions.assertSame(compact, compact.withoutRawOutcomes())
        );
    }

    @Test
    void cellParallelismIsDeterministicOrderedAndStoresOnlyCompactReports() {
        final var cells = Step12CombatMatrixSimulator.cells(20_260_912L, "V2").stream()
            .filter(it -> it.family() == MatrixFamily.FAVORABLE)
            .limit(3)
            .toList();

        final var sequential = Step12CombatMatrixSimulator.runCells(cells, 5, 10_000, 1);
        final var parallel = Step12CombatMatrixSimulator.runCells(cells, 5, 10_000, 3);

        for (int index = 0; index < cells.size(); index++) {
            assertEquivalentResult(sequential.get(index), parallel.get(index));
        }
        Assertions.assertAll(
            () -> Assertions.assertEquals(cells, parallel.stream().map(it -> it.cell()).toList()),
            () -> Assertions.assertTrue(parallel.stream().allMatch(it ->
                it.report().pairOutcomes().isEmpty()
                    && it.report().evaluatedFirst().iterationWins().isEmpty()
                    && it.report().opponentsFirst().iterationWins().isEmpty()
            )),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CombatMatrixSimulator.runCells(cells, 5, 10_000, 0)
            )
        );
    }

    private static void assertEquivalentResult(
        Step12CombatMatrixSimulator.CellResult expected,
        Step12CombatMatrixSimulator.CellResult actual
    ) {
        final var expectedReport = expected.report();
        final var actualReport = actual.report();
        Assertions.assertAll(
            expected.cell().code(),
            () -> Assertions.assertEquals(expected.cell(), actual.cell()),
            () -> Assertions.assertEquals(expected.decision(), actual.decision()),
            () -> Assertions.assertEquals(expectedReport.evaluatedFirst(), actualReport.evaluatedFirst()),
            () -> Assertions.assertEquals(expectedReport.opponentsFirst(), actualReport.opponentsFirst()),
            () -> Assertions.assertEquals(expectedReport.pairOutcomes(), actualReport.pairOutcomes()),
            () -> Assertions.assertEquals(expectedReport.evaluatedWins(), actualReport.evaluatedWins()),
            () -> Assertions.assertEquals(expectedReport.battles(), actualReport.battles()),
            () -> Assertions.assertEquals(expectedReport.evaluatedWinRate(), actualReport.evaluatedWinRate()),
            () -> Assertions.assertEquals(expectedReport.evaluatedWinRate95(), actualReport.evaluatedWinRate95()),
            () -> Assertions.assertEquals(expectedReport.firstSideWinRate(), actualReport.firstSideWinRate()),
            () -> Assertions.assertEquals(expectedReport.bootstrapSeed(), actualReport.bootstrapSeed())
        );
    }

    @Test
    void simulationCapturesActualStartingPositionsAfterFreeApproach() {
        final var request = new CombatSimulationRequest(
            "STEP_12",
            "FREE_APPROACH",
            "сближение",
            1,
            2,
            2,
            10_000,
            12_345,
            () -> new CombatSimulationTeams(
                List.of(
                    personageAt(RoleBuild.BRUISER, Position.FRONT, "Передний громила"),
                    personageAt(RoleBuild.BRUISER, Position.BACK, "Задний громила")
                ),
                List.of(
                    personageAt(RoleBuild.GUARDIAN, Position.FRONT, "Первый страж"),
                    personageAt(RoleBuild.GUARDIAN, Position.FRONT, "Второй страж")
                )
            )
        );

        final var report = new CombatSimulator().run(request);
        final var moved = report.startingPositions().get(1);

        Assertions.assertAll(
            () -> Assertions.assertEquals(4, report.startingPositions().size()),
            () -> Assertions.assertEquals("Задний громила", moved.participant().name()),
            () -> Assertions.assertEquals(Position.BACK, moved.requestedLine()),
            () -> Assertions.assertEquals(Position.FRONT, moved.actualLine()),
            () -> Assertions.assertEquals(2, moved.actualLineIndex()),
            () -> Assertions.assertEquals(1, moved.distanceToNearestEnemy()),
            () -> Assertions.assertEquals(1, moved.range()),
            () -> Assertions.assertTrue(report.markdown().contains("Фактическая линия после сближения")),
            () -> Assertions.assertTrue(report.markdown().contains("BACK | FRONT | 2 | 1 | 1"))
        );
    }

    @Test
    void pairOutcomesPreserveMatchingIterationIndexesAndInvertTheSecondFirstSide() {
        final var paired = PairedCombatSimulator.pairOutcomes(
            List.of(true, true, false, false),
            List.of(false, true, false, true)
        );

        Assertions.assertEquals(
            List.of(
                new PairOutcome(0, true, true),
                new PairOutcome(1, true, false),
                new PairOutcome(2, false, true),
                new PairOutcome(3, false, false)
            ),
            paired
        );
        Assertions.assertEquals(List.of(1.0, 0.5, 0.5, 0.0), paired.stream().map(PairOutcome::score).toList());
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PairedCombatSimulator.pairOutcomes(List.of(true), List.of())
        );
    }

    @Test
    void pairedBootstrapIsReproducibleAndUsesExactPreregisteredRanks() {
        final var pairs = List.of(
            new PairOutcome(0, false, false),
            new PairOutcome(1, true, false),
            new PairOutcome(2, true, true)
        );
        final var first = PairedCombatSimulator.pairedBootstrap95(pairs, 123_456L);
        final var second = PairedCombatSimulator.pairedBootstrap95(pairs, 123_456L);
        final var ordered = IntStream.range(0, PairedCombatSimulator.BOOTSTRAP_RESAMPLES)
            .mapToDouble(index -> index / (double) PairedCombatSimulator.BOOTSTRAP_RESAMPLES)
            .toArray();
        final var exactRanks = PairedCombatSimulator.bootstrapPercentile95(ordered);

        Assertions.assertAll(
            () -> Assertions.assertEquals(first, second),
            () -> Assertions.assertEquals(
                499 / (double) PairedCombatSimulator.BOOTSTRAP_RESAMPLES,
                exactRanks.lower()
            ),
            () -> Assertions.assertEquals(
                19_499 / (double) PairedCombatSimulator.BOOTSTRAP_RESAMPLES,
                exactRanks.upper()
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PairedCombatSimulator.bootstrapPercentile95(new double[10])
            )
        );
    }

    @Test
    void multinomialOutcomeBootstrapMatchesTheEmpiricalThreeCategoryDistributionAndEdges() {
        final var categories = List.of(
            new PairOutcome(0, false, false),
            new PairOutcome(1, true, false),
            new PairOutcome(2, true, true)
        );
        final var estimates = PairedCombatSimulator.pairedBootstrapEstimates(categories, 987_654L);
        final double mean = Arrays.stream(estimates).average().orElseThrow();
        final double variance = Arrays.stream(estimates)
            .map(value -> Math.pow(value - mean, 2))
            .average()
            .orElseThrow();
        final double allLosses = Arrays.stream(estimates).filter(value -> value == 0).count()
            / (double) estimates.length;
        final double allWins = Arrays.stream(estimates).filter(value -> value == 1).count()
            / (double) estimates.length;
        final var allHalf = IntStream.range(0, 10)
            .mapToObj(index -> new PairOutcome(index, true, false))
            .toList();
        final var allHalfInterval = PairedCombatSimulator.pairedBootstrap95(allHalf, 1L);

        Assertions.assertAll(
            () -> Assertions.assertEquals(0.5, mean, 0.01),
            () -> Assertions.assertEquals(1.0 / 18, variance, 0.005),
            () -> Assertions.assertEquals(1.0 / 27, allLosses, 0.01),
            () -> Assertions.assertEquals(1.0 / 27, allWins, 0.01),
            () -> Assertions.assertEquals(new ConfidenceInterval(0.5, 0.5), allHalfInterval)
        );
    }

    @Test
    void favorableOutcomeUsesTheWholePairedInterval() {
        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.FAVORABLE, 0, 0.60, 0.50, new ConfidenceInterval(0.55, 0.65)
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.FAVORABLE, 10, 0.60, 0.50, new ConfidenceInterval(0.5499, 0.64)
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.FAVORABLE, 20, 0.60, 0.50, new ConfidenceInterval(0.56, 0.6501)
            )),
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.FAVORABLE, 3, 0.51, 0.50, new ConfidenceInterval(0.5001, 0.52)
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.FAVORABLE, 6, 0.51, 0.50, new ConfidenceInterval(0.50, 0.52)
            ))
        );
    }

    @Test
    void preflightUsesPointEstimatesWithoutPretendingTwoThousandPairsHaveFinalPrecision() {
        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.preflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.47, 0.53
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.preflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.4699, 0.50
            )),
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.preflightOutcomeAccepted(
                MatrixFamily.FAVORABLE, 10, 0.55, 0.90
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.preflightOutcomeAccepted(
                MatrixFamily.FAVORABLE, 20, 0.6501, 0.50
            )),
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.preflightOutcomeAccepted(
                MatrixFamily.FAVORABLE, 3, 0.5001, 0.50
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.preflightOutcomeAccepted(
                MatrixFamily.FAVORABLE, 6, 0.50, 0.50
            ))
        );
    }

    @Test
    void v4PreflightWidensOnlyBothMirrorPointCorridorsAndKeepsFinalStrict() {
        final var interval = new ConfidenceInterval(0.49, 0.51);

        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.v4PreflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.45, 0.55
            )),
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.v4PreflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.55, 0.45
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.v4PreflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.4499, 0.50
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.v4PreflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.5501, 0.50
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.v4PreflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.50, 0.4499
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.v4PreflightOutcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.50, 0.5501
            )),
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.47, 0.53, interval
            )),
            () -> Assertions.assertTrue(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.53, 0.47, interval
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.4699, 0.50, interval
            )),
            () -> Assertions.assertFalse(Step12CombatMatrixSimulator.outcomeAccepted(
                MatrixFamily.MIRROR, 0, 0.50, 0.5301, interval
            ))
        );
    }

    @Test
    void finalV2ConfigurationIsExactWhileV2DiagnosticAllowsAnotherRootAndFilters() {
        final var valid = new MatrixRunConfiguration(
            "V2", "FINAL", 10_000, 10_000, 2_026_091_802L, true, Set.of()
        );
        final var diagnostic = new MatrixRunConfiguration(
            "V2", "DIAGNOSTIC", 37, 41, 2_026_091_801L, false, Set.of("scenario", "levels")
        );

        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(() -> Step12CombatMatrixSimulator.validateRunConfiguration(valid)),
            () -> Assertions.assertDoesNotThrow(() ->
                Step12CombatMatrixSimulator.validateRunConfiguration(diagnostic)
            ),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "FINAL", 9_999, 10_000, 2_026_091_802L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "FINAL", 10_000, 10_000, 2_026_091_801L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "FINAL", 10_000, 10_000, 2_026_091_802L, false, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "FINAL", 10_000, 10_000, 2_026_091_802L, true, Set.of("scenario")
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "FINAL", 10_000, 9_999, 2_026_091_802L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "DIAGNOSTIC", 10_000, 10_000, 2_026_091_802L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V1", "FINAL", 10_000, 10_000, 2_026_091_802L, true, Set.of()
            )),
            () -> Assertions.assertDoesNotThrow(() -> Step12CombatMatrixSimulator.validateFinalCells(
                Step12CombatMatrixSimulator.cells(2_026_091_802L, "V2")
            )),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CombatMatrixSimulator.validateFinalCells(
                    Step12CombatMatrixSimulator.cells(1L, "V1")
                )
            )
        );
    }

    @Test
    void revisionThreePreflightAndFinalConfigurationsAreOneUseAndExact() {
        final var preflight = new MatrixRunConfiguration(
            "V3", "PREFLIGHT", 2_000, 10_000, 2_026_092_201L, true, Set.of()
        );
        final var finalRun = new MatrixRunConfiguration(
            "V3", "FINAL", 10_000, 10_000, 2_026_092_301L, true, Set.of()
        );

        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(() ->
                Step12CombatMatrixSimulator.validateRunConfiguration(preflight)
            ),
            () -> Assertions.assertDoesNotThrow(() ->
                Step12CombatMatrixSimulator.validateRunConfiguration(finalRun)
            ),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "DIAGNOSTIC", 2_000, 10_000, 2_026_092_201L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "DIAGNOSTIC", 2_000, 10_000, 2_026_092_202L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "DIAGNOSTIC", 10_000, 10_000, 2_026_092_301L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "PREFLIGHT", 2_000, 10_000, 2_026_092_201L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "PREFLIGHT", 1_999, 10_000, 2_026_092_201L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "PREFLIGHT", 2_000, 9_999, 2_026_092_201L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "PREFLIGHT", 2_000, 10_000, 2_026_092_202L, true, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "PREFLIGHT", 2_000, 10_000, 2_026_092_201L, false, Set.of()
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "PREFLIGHT", 2_000, 10_000, 2_026_092_201L, true, Set.of("levels")
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "FINAL", 10_000, 10_000, 2_026_092_201L, true, Set.of()
            ))
        );
    }

    @Test
    void acceptanceProtocolFourUsesNewRootsWhileKeepingGameplayRevisionThree() {
        final var preflight = new MatrixRunConfiguration(
            "V3", "PREFLIGHT", 2_000, 10_000, 2_026_092_203L, true, Set.of(), "V4"
        );
        final var finalRun = new MatrixRunConfiguration(
            "V3", "FINAL", 10_000, 10_000, 2_026_092_302L, true, Set.of(), "V4"
        );

        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(() ->
                Step12CombatMatrixSimulator.validateRunConfiguration(preflight)
            ),
            () -> Assertions.assertDoesNotThrow(() ->
                Step12CombatMatrixSimulator.validateRunConfiguration(finalRun)
            ),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "PREFLIGHT", 2_000, 10_000, 2_026_092_201L, true, Set.of(), "V4"
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "FINAL", 10_000, 10_000, 2_026_092_301L, true, Set.of(), "V4"
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "PREFLIGHT", 2_000, 10_000, 2_026_092_203L, true, Set.of(), "V5"
            )),
            () -> assertInvalidFinal(new MatrixRunConfiguration(
                "V2", "FINAL", 10_000, 10_000, 2_026_091_802L, true, Set.of(), "V4"
            ))
        );
    }

    @Test
    void diagnosticModeRejectsEveryPublishedAcceptanceRoot() {
        final var reservedRoots = List.of(
            2_026_091_802L,
            2_026_092_201L,
            2_026_092_202L,
            2_026_092_301L,
            2_026_092_203L,
            2_026_092_204L,
            2_026_092_302L
        );

        for (final long root : reservedRoots) {
            assertInvalidFinal(new MatrixRunConfiguration(
                "V3", "DIAGNOSTIC", 1, 10_000, root, false, Set.of(), "V4"
            ));
        }
    }

    private static void assertInvalidFinal(MatrixRunConfiguration configuration) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12CombatMatrixSimulator.validateRunConfiguration(configuration)
        );
    }

    private static ExpectedStats stats(
        int health,
        DefenseType defenseType,
        int defense,
        AttackType attackType,
        int rangeOne,
        int rangeTwo,
        int rangeThree,
        int rangeFour,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int threat,
        int impact
    ) {
        return new ExpectedStats(
            health,
            defenseType,
            defense,
            attackType,
            List.of(rangeOne, rangeTwo, rangeThree, rangeFour),
            critChance,
            dodgeChance,
            critMultiplier,
            speed,
            threat,
            impact
        );
    }

    private static void assertStats(RoleBuild build, BattlePersonage personage, ExpectedStats expected) {
        Assertions.assertAll(
            build.name(),
            () -> Assertions.assertEquals(expected.health(), personage.maxHealth()),
            () -> Assertions.assertEquals(Map.of(expected.defenseType(), expected.defense()), personage.defenses()),
            () -> Assertions.assertEquals(expected.attackByRange(), attackByRange(personage, expected.attackType())),
            () -> Assertions.assertEquals(expected.critChance(), personage.critChance()),
            () -> Assertions.assertEquals(expected.dodgeChance(), personage.dodgeChance()),
            () -> Assertions.assertEquals(expected.critMultiplier(), personage.critMultiplier(), 1e-9),
            () -> Assertions.assertEquals(expected.speed(), personage.initiative()),
            () -> Assertions.assertEquals(expected.threat(), personage.totalThreat()),
            () -> Assertions.assertEquals(expected.impact(), personage.impactStrength())
        );
    }

    private static List<Integer> attackByRange(BattlePersonage personage, AttackType attackType) {
        return List.of(1, 2, 3, 4).stream()
            .map(distance -> distance <= personage.range()
                ? personage.attackAtRange(distance).getOrDefault(attackType, 0)
                : 0)
            .toList();
    }

    private static List<String> names(List<BattlePersonage> team) {
        return team.stream().map(it -> it.name().orElseThrow()).toList();
    }

    private static List<Position> positions(List<BattlePersonage> team) {
        return team.stream().map(BattlePersonage::startPosition).toList();
    }

    private static BattlePersonage personageAt(RoleBuild build, Position position, String name) {
        return new BattlePersonage(
            Step12SimulationFixtures.items(build, 0),
            position,
            Map.of(),
            PersonageEffects.EMPTY,
            null,
            Optional.of(name),
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }

    private static long countFamily(
        List<Step12CombatMatrixSimulator.MatrixCell> cells,
        MatrixFamily family
    ) {
        return cells.stream().filter(cell -> cell.family() == family).count();
    }

    private record ExpectedStats(
        int health,
        DefenseType defenseType,
        int defense,
        AttackType attackType,
        List<Integer> attackByRange,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int threat,
        int impact
    ) {
    }
}
