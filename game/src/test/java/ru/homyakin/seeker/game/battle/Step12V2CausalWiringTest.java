package ru.homyakin.seeker.game.battle;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.item.models.AttackType;

/**
 * Deterministic preflight for seven roles in the six revision-two directed matchups.
 *
 * <p>This suite deliberately reports no wins or win rates. It proves, on the separately preregistered
 * {@code 2026091801} root, that every exact composition can physically create its named causal opportunity before
 * the final sample is run.</p>
 */
class Step12V2CausalWiringTest {
    @Test
    void guardianThreeByThreeInterceptsPositiveDamageThatControlWardReceives() {
        final var active = Step12V2CausalTestSupport.scenario(V2Matchup.BRUISER_V2);
        final var control = Step12V2CausalTestSupport.scenario(
            V2Matchup.BRUISER_V2,
            0,
            Optional.of(V2Build.GUARDIAN_BLUNT)
        );
        final var activeAttacker = supportAt(active.teams().evaluatedTeam(), Position.FRONT);
        final var controlAttacker = supportAt(control.teams().evaluatedTeam(), Position.FRONT);
        final var activeWard = supportAt(active.teams().opponents(), Position.MID);
        final var controlWard = supportAt(control.teams().opponents(), Position.MID);
        final var activeGuardian = Step12V2CausalTestSupport.named(
            active.teams().opponents(),
            V2Build.GUARDIAN_BLUNT
        );
        active.random().forceTarget(activeWard);
        control.random().forceTarget(controlWard);

        activeAttacker.move(active.context(), active.log(), 1);
        controlAttacker.move(control.context(), control.log(), 1);

        final var activeAttempt = onlyAttempt(active, activeAttacker);
        final var controlAttempt = onlyAttempt(control, controlAttacker);
        Assertions.assertAll(
            () -> Assertions.assertEquals(activeWard.id(), activeAttempt.originalTargetId()),
            () -> Assertions.assertEquals(activeGuardian.id(), activeAttempt.finalTargetId()),
            () -> Assertions.assertTrue(activeAttempt.normalDamage() > 0),
            () -> Assertions.assertEquals(activeWard.maxHealth(), activeWard.health()),
            () -> Assertions.assertEquals(controlWard.id(), controlAttempt.originalTargetId()),
            () -> Assertions.assertEquals(controlWard.id(), controlAttempt.finalTargetId()),
            () -> Assertions.assertTrue(controlAttempt.normalDamage() > 0),
            () -> Assertions.assertTrue(controlWard.health() < controlWard.maxHealth()),
            () -> Assertions.assertEquals(1, Step12V2CausalTestSupport.events(
                active.log(),
                BattleEvent.AttackIntercepted.class
            ).size()),
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.events(
                control.log(),
                BattleEvent.AttackIntercepted.class
            ).isEmpty())
        );
    }

    @Test
    void bruiserCompositionCreatesExactBerserkAttackAgainstGuardian() {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.BRUISER_V2);
        final var bruiser = Step12V2CausalTestSupport.named(
            scenario.teams().evaluatedTeam(),
            V2Build.BRUISER_BLUNT
        );
        final var guardian = Step12V2CausalTestSupport.named(
            scenario.teams().opponents(),
            V2Build.GUARDIAN_BLUNT
        );
        final int threshold = Math.floorDiv(Math.multiplyExact(3, bruiser.maxHealth()), 10);
        Step12V2CausalTestSupport.setHealth(bruiser, threshold);
        scenario.random().forceTarget(guardian);

        bruiser.move(scenario.context(), scenario.log(), 1);

        final var attempt = onlyAttempt(scenario, bruiser);
        Assertions.assertAll(
            () -> Assertions.assertEquals(1, bruiser.distanceTo(guardian)),
            () -> Assertions.assertEquals(guardian.id(), attempt.originalTargetId()),
            () -> Assertions.assertEquals(AttackAccess.NORMAL, attempt.access()),
            () -> Assertions.assertEquals(Map.of(AttackType.BLUNT, 588), attempt.savedBasis()),
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.usedSkill(
                scenario.log(),
                bruiser.id(),
                ActiveEnum.BERSERK
            ))
        );
    }

    @Test
    void skirmisherCompositionCreatesRetreatAndBreakerMoveOnlyTurn() {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.SKIRMISHER_V2);
        final var skirmisher = Step12V2CausalTestSupport.named(
            scenario.teams().evaluatedTeam(),
            V2Build.SKIRMISHER_SLASH
        );
        final var breaker = Step12V2CausalTestSupport.named(
            scenario.teams().opponents(),
            V2Build.BREAKER_CLOSE_SLASH
        );
        scenario.random().forceTarget(breaker);

        skirmisher.move(scenario.context(), scenario.log(), 1);
        final var retreat = onlyAttempt(scenario, skirmisher);
        final int chargesBeforePursuit = breaker.scalingSkills().accumulationCharges();
        scenario.random().forceTarget(skirmisher);
        breaker.move(scenario.context(), scenario.log(), 2);

        final var breakerTurn = Step12V2CausalTestSupport.traces(
            scenario.log(),
            BattleTraceEvent.TurnFinished.class
        ).stream().filter(event -> event.personageId().equals(breaker.id())).findFirst().orElseThrow();
        Assertions.assertAll(
            () -> Assertions.assertEquals(AttackAccess.HIT_AND_RUN, retreat.access()),
            () -> Assertions.assertEquals(retreat.lineBeforeRetreat() - 1, retreat.lineAfterRetreat()),
            () -> Assertions.assertEquals(
                breakerTurn.startLineIndex() - 1,
                breakerTurn.endLineIndex()
            ),
            () -> Assertions.assertEquals(0, Step12V2CausalTestSupport.attemptsBy(scenario.log(), breaker)),
            () -> Assertions.assertEquals(
                chargesBeforePursuit,
                breaker.scalingSkills().accumulationCharges()
            )
        );
    }

    @Test
    void assassinCompositionCreatesNamedDistanceThreePenetrationAttempt() {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.ASSASSIN_V2);
        final var assassin = Step12V2CausalTestSupport.named(
            scenario.teams().evaluatedTeam(),
            V2Build.ASSASSIN_PIERCE
        );
        final var ranger = Step12V2CausalTestSupport.named(
            scenario.teams().opponents(),
            V2Build.RANGER_PIERCE
        );
        scenario.random().forceTarget(ranger);

        assassin.move(scenario.context(), scenario.log(), 1);

        final var attempt = onlyAttempt(scenario, assassin);
        Assertions.assertAll(
            () -> Assertions.assertEquals(ranger.id(), attempt.originalTargetId()),
            () -> Assertions.assertEquals(ranger.id(), attempt.finalTargetId()),
            () -> Assertions.assertEquals(3, attempt.distance()),
            () -> Assertions.assertEquals(2, attempt.ordinaryRange()),
            () -> Assertions.assertEquals(AttackAccess.PENETRATION, attempt.access()),
            () -> Assertions.assertTrue(Step12V2CausalTestSupport.usedSkill(
                scenario.log(),
                assassin.id(),
                ActiveEnum.PENETRATION
            ))
        );
    }

    @Test
    void rangerCompositionCreatesNamedFarAttempt() {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.RANGER_V2);
        final var ranger = Step12V2CausalTestSupport.named(
            scenario.teams().evaluatedTeam(),
            V2Build.RANGER_MAGICAL
        );
        final var bruiser = Step12V2CausalTestSupport.named(
            scenario.teams().opponents(),
            V2Build.BRUISER_MAGICAL_CLOTH
        );
        scenario.random().forceTarget(bruiser);

        ranger.move(scenario.context(), scenario.log(), 1);

        final var attempt = onlyAttempt(scenario, ranger);
        Assertions.assertAll(
            () -> Assertions.assertEquals(bruiser.id(), attempt.originalTargetId()),
            () -> Assertions.assertEquals(3, attempt.distance()),
            () -> Assertions.assertEquals(Map.of(AttackType.MAGICAL, 480), attempt.savedBasis()),
            () -> Assertions.assertTrue(attempt.normalDamage() > 0)
        );
    }

    @Test
    void breakerCompositionCreatesFourthAttemptDischarge() {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.BREAKER_V2);
        final var breaker = Step12V2CausalTestSupport.named(
            scenario.teams().evaluatedTeam(),
            V2Build.BREAKER_MAGICAL_LEATHER
        );
        final var bruiser = Step12V2CausalTestSupport.named(
            scenario.teams().opponents(),
            V2Build.BRUISER_MAGICAL_LEATHER
        );
        scenario.random().forceTarget(bruiser);

        for (int turn = 1; turn <= 4; turn++) {
            breaker.move(scenario.context(), scenario.log(), turn);
        }

        final var attempts = attempts(scenario, breaker);
        final var discharge = attempts.getLast();
        Assertions.assertAll(
            () -> Assertions.assertEquals(4, attempts.size()),
            () -> Assertions.assertTrue(bruiser.isAlive(), "the named target must survive the first three attempts"),
            () -> Assertions.assertEquals(3, discharge.accumulationChargesBefore()),
            () -> Assertions.assertEquals(0, discharge.accumulationChargesAfter()),
            () -> Assertions.assertTrue(discharge.discharge()),
            () -> Assertions.assertEquals(Map.of(AttackType.MAGICAL, 420), discharge.dischargeBasis()),
            () -> Assertions.assertEquals(18, discharge.dischargeCoefficientNumerator()),
            () -> Assertions.assertEquals(25, discharge.dischargeCoefficientDenominator()),
            () -> Assertions.assertTrue(discharge.dischargeDamage() > 0)
        );
    }

    @Test
    void tacticianCompositionCreatesExactNamedInitiativeDelay() {
        final var scenario = Step12V2CausalTestSupport.scenario(V2Matchup.TACTICIAN_V2);
        final var tactician = Step12V2CausalTestSupport.named(
            scenario.teams().evaluatedTeam(),
            V2Build.TACTICIAN_MAGICAL
        );
        final var bruiser = Step12V2CausalTestSupport.named(
            scenario.teams().opponents(),
            V2Build.BRUISER_MAGICAL_CLOTH
        );
        bruiser.setInitiativeGaugeForTest(500);
        scenario.random().forceTarget(bruiser);

        tactician.move(scenario.context(), scenario.log(), 1);

        final var delay = Step12V2CausalTestSupport.events(
            scenario.log(),
            BattleEvent.InitiativeDelayed.class
        ).getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(bruiser.id(), delay.targetId()),
            () -> Assertions.assertEquals(tactician.id(), delay.sourceId()),
            () -> Assertions.assertEquals(83, delay.amount()),
            () -> Assertions.assertEquals(417, delay.gaugeAfter()),
            () -> Assertions.assertEquals(2, tactician.scalingSkills().cooldown(ActiveEnum.TEMPO_BREAK))
        );
    }

    private static BattleTraceEvent.NormalAttackAttempt onlyAttempt(
        Step12V2CausalTestSupport.Scenario scenario,
        BattlePersonage personage
    ) {
        return attempts(scenario, personage).getFirst();
    }

    private static java.util.List<BattleTraceEvent.NormalAttackAttempt> attempts(
        Step12V2CausalTestSupport.Scenario scenario,
        BattlePersonage personage
    ) {
        return Step12V2CausalTestSupport.traces(
            scenario.log(),
            BattleTraceEvent.NormalAttackAttempt.class
        ).stream().filter(attempt -> attempt.attackerId().equals(personage.id())).toList();
    }

    private static BattlePersonage supportAt(
        java.util.List<BattlePersonage> team,
        Position position
    ) {
        return Step12V2CausalTestSupport.namedAll(team, V2Build.SUPPORT).stream()
            .filter(personage -> personage.startPosition() == position)
            .findFirst()
            .orElseThrow();
    }
}
