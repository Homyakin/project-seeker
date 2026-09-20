package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.item.models.AttackType;

class Step12V3ObservationCorrectionTest {
    private static final long CHECK_SEED = 2_026_092_096L;
    private static final UUID ATTACKER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ACTIVE_WARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CONTROL_WARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID GUARDIAN_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void hitAndRunObservationAcceptsRetreatForBothBattleSideOrders() {
        final var firstTeamRetreat = attempt(
            ACTIVE_WARD_ID,
            ACTIVE_WARD_ID,
            false,
            1,
            2,
            1
        );
        final var secondTeamRetreat = attempt(
            ACTIVE_WARD_ID,
            ACTIVE_WARD_ID,
            false,
            1,
            3,
            4
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(Step12CausalObservationRunner.retreatedBackward(
                firstTeamRetreat,
                BattleAdvanceDirection.TOWARD_SECOND_TEAM
            )),
            () -> Assertions.assertTrue(Step12CausalObservationRunner.retreatedBackward(
                secondTeamRetreat,
                BattleAdvanceDirection.TOWARD_FIRST_TEAM
            ))
        );
    }

    @Test
    void hitAndRunCausalObservationWorksInBothActualSideOrders() {
        final var seed = java.util.stream.LongStream.range(CHECK_SEED, CHECK_SEED + 64)
            .filter(candidate -> Step12V3CausalObservationRunner.skirmisherSide(
                0,
                candidate,
                false,
                10_000
            ))
            .filter(candidate -> Step12V3CausalObservationRunner.skirmisherSide(
                0,
                candidate,
                true,
                10_000
            ))
            .findFirst();

        Assertions.assertTrue(seed.isPresent(), "the deterministic check range must cover both side orders");
    }

    @Test
    void guardianOpportunityCountsAnActiveDodgeWhenTheControlWardWouldBeHit() {
        final var activeDodge = attempt(
            ACTIVE_WARD_ID,
            GUARDIAN_ID,
            true,
            0,
            2,
            2
        );
        final var controlHit = attempt(
            CONTROL_WARD_ID,
            CONTROL_WARD_ID,
            false,
            10,
            2,
            2
        );

        Assertions.assertTrue(Step12CausalObservationRunner.usefulGuardianOpportunity(
            List.of(activeDodge),
            List.of(controlHit),
            ACTIVE_WARD_ID,
            GUARDIAN_ID,
            CONTROL_WARD_ID
        ));
    }

    @Test
    void guardianOpportunityRejectsAControlDodgeEvenWhenALaterOpportunityHits() {
        final var activeHit = attempt(
            ACTIVE_WARD_ID,
            GUARDIAN_ID,
            false,
            10,
            2,
            2
        );
        final var controlDodge = attempt(
            CONTROL_WARD_ID,
            CONTROL_WARD_ID,
            true,
            0,
            2,
            2
        );
        final var laterActiveHit = attempt(
            ACTIVE_WARD_ID,
            GUARDIAN_ID,
            false,
            10,
            2,
            2,
            2
        );
        final var laterControlHit = attempt(
            CONTROL_WARD_ID,
            CONTROL_WARD_ID,
            false,
            10,
            2,
            2,
            2
        );

        Assertions.assertFalse(Step12CausalObservationRunner.usefulGuardianOpportunity(
            List.of(activeHit, laterActiveHit),
            List.of(controlDodge, laterControlHit),
            ACTIVE_WARD_ID,
            GUARDIAN_ID,
            CONTROL_WARD_ID
        ));
    }

    @Test
    void causalActionCounterIgnoresADeadQueuedTurnButKeepsAnActionEndingInDeath() {
        final var noAction = new BattleTraceEvent.TurnFinished(
            1,
            ACTIVE_WARD_ID,
            1,
            2,
            2,
            false,
            false,
            1
        );
        final var actionEndingInDeath = new BattleTraceEvent.TurnFinished(
            2,
            ACTIVE_WARD_ID,
            1,
            2,
            2,
            true,
            false,
            1
        );

        Assertions.assertEquals(
            1,
            Step12V3CausalObservationRunner.performedActionsBy(
                List.of(noAction, actionEndingInDeath),
                ACTIVE_WARD_ID
            )
        );
        Assertions.assertEquals(
            actionEndingInDeath,
            Step12V3CausalObservationRunner.nextPerformedTurn(
                List.of(noAction, actionEndingInDeath),
                ACTIVE_WARD_ID,
                0
            ).orElseThrow()
        );
    }

    private static BattleTraceEvent.NormalAttackAttempt attempt(
        UUID originalTargetId,
        UUID finalTargetId,
        boolean dodged,
        int damage,
        int lineBeforeRetreat,
        int lineAfterRetreat
    ) {
        return attempt(
            originalTargetId,
            finalTargetId,
            dodged,
            damage,
            lineBeforeRetreat,
            lineAfterRetreat,
            1
        );
    }

    private static BattleTraceEvent.NormalAttackAttempt attempt(
        UUID originalTargetId,
        UUID finalTargetId,
        boolean dodged,
        int damage,
        int lineBeforeRetreat,
        int lineAfterRetreat,
        int ownTurn
    ) {
        return new BattleTraceEvent.NormalAttackAttempt(
            ownTurn,
            ownTurn,
            ATTACKER_ID,
            ownTurn,
            originalTargetId,
            finalTargetId,
            AttackAccess.NORMAL,
            1,
            1,
            Map.of(AttackType.SLASH, 10),
            false,
            dodged,
            damage,
            false,
            false,
            Map.of(),
            0,
            1,
            0,
            0,
            0,
            lineBeforeRetreat,
            lineAfterRetreat,
            1
        );
    }
}
