package ru.homyakin.seeker.game.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Step12OneShotBattleRandomTest {
    private static final long SEED = 2_026_091_803L;

    @Test
    void forcesTargetOnlyAtFirstEligibleTargetSelectionAndKeepsDelegateAligned() {
        final var forcedTarget = UUID.fromString("00000000-0000-0000-0000-000000000001");
        final var otherTarget = UUID.fromString("00000000-0000-0000-0000-000000000002");
        final var absentTarget = UUID.fromString("00000000-0000-0000-0000-000000000003");
        final var random = new Step12OneShotBattleRandom(SEED, forcedTarget);
        final var baseline = new SeededBattleRandom(SEED);
        final var withoutForcedTarget = weights(otherTarget, absentTarget);
        final var withForcedTarget = weights(forcedTarget, otherTarget);
        final var sequence = "target-selection:attacker";

        Assertions.assertEquals(
            baseline.pickWeighted("unrelated-selection", withForcedTarget),
            random.pickWeighted("unrelated-selection", withForcedTarget)
        );
        Assertions.assertEquals(
            baseline.pickWeighted(sequence, withoutForcedTarget),
            random.pickWeighted(sequence, withoutForcedTarget)
        );
        baseline.pickWeighted(sequence, withForcedTarget);
        Assertions.assertEquals(forcedTarget, random.pickWeighted(sequence, withForcedTarget));

        final var expectedDelegatedSelection = baseline.pickWeighted(sequence, withForcedTarget);
        Assertions.assertNotEquals(forcedTarget, expectedDelegatedSelection);
        Assertions.assertEquals(
            expectedDelegatedSelection,
            random.pickWeighted(sequence, withForcedTarget)
        );
    }

    @Test
    void forcesOnlyRequestedNormalDodgeAndKeepsDelegateAligned() {
        final var random = new Step12OneShotBattleRandom(SEED);
        final var baseline = new SeededBattleRandom(SEED);
        final var criticalSequence = "normal-critical:attacker";
        final var dodgeSequence = "normal-dodge:target:attacker";
        random.forceDodgeOnAttempt(3);

        Assertions.assertEquals(
            baseline.nextInt(criticalSequence, 1, 10_000),
            random.nextInt(criticalSequence, 1, 10_000)
        );
        for (int attempt = 1; attempt <= 5; attempt++) {
            final int delegated = baseline.nextInt(dodgeSequence, 1, 10_000);
            final int actual = random.nextInt(dodgeSequence, 1, 10_000);
            if (attempt == 3) {
                Assertions.assertEquals(1, actual);
            } else {
                Assertions.assertEquals(delegated, actual);
            }
        }
    }

    @Test
    void rejectsNonPositiveDodgeAttempt() {
        final var random = new Step12OneShotBattleRandom(SEED);

        Assertions.assertAll(
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> random.forceDodgeOnAttempt(0)
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> random.forceDodgeOnAttempt(-1)
            )
        );
    }

    private static Map<UUID, Integer> weights(UUID first, UUID second) {
        final var weights = new LinkedHashMap<UUID, Integer>();
        weights.put(first, 1);
        weights.put(second, 1_000_000);
        return weights;
    }
}
