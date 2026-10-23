package ru.homyakin.seeker.game.battle;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Cell;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Metric;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;

class Step12MixedV2ObservationRunnerTest {
    private static final long ROOT_SEED = 2_026_100_202L;
    private static final int MAX_ROUNDS = 10_000;

    @Test
    void iterationSeedsAreDeterministicDistinctAndRejectNegativeIterations() {
        final long cellSeed = cell("RAM_PRESSURE", 0).seed();
        final long first = Step12MixedV2ObservationRunner.iterationSeed(cellSeed, 0);
        final long repeated = Step12MixedV2ObservationRunner.iterationSeed(cellSeed, 0);
        final long next = Step12MixedV2ObservationRunner.iterationSeed(cellSeed, 1);

        Assertions.assertAll(
            () -> Assertions.assertEquals(first, repeated),
            () -> Assertions.assertNotEquals(first, next),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedV2ObservationRunner.iterationSeed(cellSeed, -1)
            )
        );
    }

    @Test
    void everyPresentWorldContainsBothTeamOrders() {
        final var activityAndReference = Step12MixedV2ObservationRunner.observe(
            cell("RAM_PRESSURE", 0),
            0,
            MAX_ROUNDS
        );
        final var referenceOnly = Step12MixedV2ObservationRunner.observe(
            cell("ARTILLERIST_RANGED", 0),
            0,
            MAX_ROUNDS
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, activityAndReference.mixedOn().battles()),
            () -> Assertions.assertEquals(2, activityAndReference.mixedOff().orElseThrow().battles()),
            () -> Assertions.assertEquals(2, activityAndReference.reference().orElseThrow().battles()),
            () -> Assertions.assertEquals(2, referenceOnly.mixedOn().battles()),
            () -> Assertions.assertTrue(referenceOnly.mixedOff().isEmpty()),
            () -> Assertions.assertEquals(2, referenceOnly.reference().orElseThrow().battles()),
            () -> assertTwoOrderCounts(activityAndReference.mixedOn()),
            () -> assertTwoOrderCounts(activityAndReference.mixedOff().orElseThrow()),
            () -> assertTwoOrderCounts(activityAndReference.reference().orElseThrow())
        );
    }

    @Test
    void guardWorldHasAttemptsAndActualInterceptions() {
        final var observation = Step12MixedV2ObservationRunner.observe(
            cell("RAM_GUARD", 0),
            0,
            MAX_ROUNDS
        );
        final var enabled = observation.mixedOn();
        final var disabled = observation.mixedOff().orElseThrow();

        Assertions.assertAll(
            () -> Assertions.assertTrue(enabled.opponentAttempts() > 0),
            () -> Assertions.assertTrue(enabled.interceptions() > 0),
            () -> Assertions.assertTrue(enabled.hasDenominator(
                Metric.GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT
            )),
            () -> Assertions.assertTrue(enabled.metric(
                Metric.GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT
            ) > 0),
            () -> Assertions.assertEquals(0, disabled.interceptions())
        );
    }

    @Test
    void blockerControlHasAllThreeWorldsAndKeepsOneKnockbackPerEligibleHit() {
        final var observation = Step12MixedV2ObservationRunner.observe(
            cell("BLOCKER_CONTROL", 0),
            0,
            MAX_ROUNDS
        );
        final var enabled = observation.mixedOn();
        final var disabled = observation.mixedOff().orElseThrow();
        final var reference = observation.reference().orElseThrow();

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, enabled.battles()),
            () -> Assertions.assertEquals(2, disabled.battles()),
            () -> Assertions.assertEquals(2, reference.battles()),
            () -> Assertions.assertTrue(enabled.knockbackLimitKept()),
            () -> Assertions.assertTrue(enabled.knockbackMoves() <= enabled.eligibleKnockbacks()),
            () -> Assertions.assertEquals(0, disabled.knockbackMoves()),
            () -> Assertions.assertEquals(0, reference.knockbackMoves()),
            () -> Assertions.assertTrue(reference.hasDenominator(
                Metric.INITIATIVE_REMOVED_PER_TURN
            ))
        );
    }

    @Test
    void disabledWorldRemovesOnlyTheSelectedMixedBuildSkill() {
        final var enabled = Step12MixedV2ObservationRunner.mixedPersonage(
            MixedBuild.RAM,
            0,
            java.util.Optional.empty()
        );
        final var berserkDisabled = Step12MixedV2ObservationRunner.mixedPersonage(
            MixedBuild.RAM,
            0,
            java.util.Optional.of(ActiveEnum.BERSERK)
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, points(enabled, ActiveEnum.BERSERK)),
            () -> Assertions.assertEquals(2, points(enabled, ActiveEnum.GUARD)),
            () -> Assertions.assertEquals(0, points(berserkDisabled, ActiveEnum.BERSERK)),
            () -> Assertions.assertEquals(2, points(berserkDisabled, ActiveEnum.GUARD)),
            () -> Assertions.assertEquals(enabled.maxHealth(), berserkDisabled.maxHealth()),
            () -> Assertions.assertEquals(enabled.defenses(), berserkDisabled.defenses())
        );
    }

    @Test
    void testedBowComesFromTheThirdCatalogWithoutChangingTheSkillFormula() {
        final var historical = Step12MixedBuildObservationRunner.mixedPersonage(
            MixedBuild.ARTILLERIST,
            0,
            java.util.Optional.empty()
        );
        final var revised = Step12MixedV2ObservationRunner.mixedPersonage(
            MixedBuild.ARTILLERIST,
            0,
            java.util.Optional.empty()
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(300, historical.attackAtRange(1).get(AttackType.PIERCE)),
            () -> Assertions.assertEquals(240, revised.attackAtRange(1).get(AttackType.PIERCE)),
            () -> Assertions.assertEquals(
                historical.skillSnapshots(),
                revised.skillSnapshots()
            )
        );
    }

    private static void assertTwoOrderCounts(Step12MixedV2ObservationRunner.PairMeasure measure) {
        Assertions.assertAll(
            () -> Assertions.assertTrue(measure.evaluatedWins() >= 0),
            () -> Assertions.assertTrue(measure.evaluatedWins() <= 2),
            () -> Assertions.assertTrue(measure.firstSideWins() >= 0),
            () -> Assertions.assertTrue(measure.firstSideWins() <= 2)
        );
    }

    private static Cell cell(String comparisonCode, int level) {
        final List<Cell> matches = Step12MixedBuildMatrix.cells(ROOT_SEED).stream()
            .filter(candidate -> candidate.comparison().code().equals(comparisonCode))
            .filter(candidate -> candidate.level() == level)
            .toList();
        return Assertions.assertDoesNotThrow(() -> matches.getFirst());
    }

    private static int points(BattlePersonage personage, ActiveEnum skill) {
        return personage.skillSnapshots().stream()
            .filter(snapshot -> snapshot.code() == skill)
            .mapToInt(BattleSkillInitSnapshot::points)
            .findFirst()
            .orElse(0);
    }
}
