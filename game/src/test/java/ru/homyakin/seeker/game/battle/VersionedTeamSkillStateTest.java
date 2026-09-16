package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VersionedTeamSkillStateTest {
    @Test
    void interceptedAttemptDoesNotDecreaseNewCooldown() {
        final var state = new VersionedTeamSkillState();

        Assertions.assertTrue(state.guardReadyForAttempt());
        state.consumeGuard(3, 3);

        Assertions.assertEquals(3, state.guardAttemptsRemaining());
    }

    @Test
    void nextAttemptAfterAllSkippedAttemptsCanBeIntercepted() {
        final var state = new VersionedTeamSkillState();
        state.consumeGuard(3, 3);

        Assertions.assertFalse(state.guardReadyForAttempt());
        Assertions.assertFalse(state.guardReadyForAttempt());
        Assertions.assertFalse(state.guardReadyForAttempt());
        Assertions.assertTrue(state.guardReadyForAttempt());
    }

    @Test
    void teamPhaseAlternatesStartingWithLowerCooldown() {
        final var state = new VersionedTeamSkillState();

        Assertions.assertEquals(4, state.consumeGuard(4, 5));
        Assertions.assertEquals(5, state.consumeGuard(4, 5));
        Assertions.assertEquals(4, state.consumeGuard(4, 5));
    }

    @Test
    void fixedOwnerStillAdvancesSharedPhaseForNextOwner() {
        final var state = new VersionedTeamSkillState();

        state.consumeGuard(4, 4);

        Assertions.assertEquals(5, state.consumeGuard(4, 5));
    }
}
