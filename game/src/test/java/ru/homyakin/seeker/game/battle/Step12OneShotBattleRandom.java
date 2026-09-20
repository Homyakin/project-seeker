package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Seeded battle random with narrowly scoped one-shot controls for step 12 experiments.
 *
 * <p>An overridden draw is still consumed from the delegate, so all later draws remain aligned with an
 * uncontrolled run that uses the same seed.</p>
 */
public final class Step12OneShotBattleRandom implements BattleRandom {
    private static final String TARGET_SELECTION_PREFIX = "target-selection:";
    private static final String NORMAL_DODGE_PREFIX = "normal-dodge:";

    private final SeededBattleRandom delegate;
    private final Optional<UUID> forcedTargetId;
    private boolean targetForced;
    private int forcedDodgeAttempt = -1;
    private int normalDodgeAttempts;

    public Step12OneShotBattleRandom(long seed) {
        this.delegate = new SeededBattleRandom(seed);
        this.forcedTargetId = Optional.empty();
    }

    public Step12OneShotBattleRandom(long seed, UUID forcedTargetId) {
        this.delegate = new SeededBattleRandom(seed);
        this.forcedTargetId = Optional.of(Objects.requireNonNull(forcedTargetId, "forcedTargetId"));
    }

    /** Forces the selected dodge roll once, counting from the next normal-dodge draw. */
    public void forceDodgeOnAttempt(int attempt) {
        if (attempt <= 0) {
            throw new IllegalArgumentException("Dodge attempt must be positive");
        }
        forcedDodgeAttempt = attempt;
        normalDodgeAttempts = 0;
    }

    @Override
    public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
        final int delegated = delegate.nextInt(sequence, minimumInclusive, maximumInclusive);
        if (!sequence.startsWith(NORMAL_DODGE_PREFIX) || forcedDodgeAttempt < 0) {
            return delegated;
        }
        normalDodgeAttempts++;
        if (normalDodgeAttempts == forcedDodgeAttempt) {
            forcedDodgeAttempt = -1;
            return minimumInclusive;
        }
        return delegated;
    }

    @Override
    public <T> T pickWeighted(String sequence, Map<T, Integer> weights) {
        final T delegated = delegate.pickWeighted(sequence, weights);
        if (targetForced || !sequence.startsWith(TARGET_SELECTION_PREFIX) || forcedTargetId.isEmpty()) {
            return delegated;
        }
        for (final var candidate : weights.keySet()) {
            if (forcedTargetId.get().equals(candidateId(candidate))) {
                targetForced = true;
                return candidate;
            }
        }
        return delegated;
    }

    @Override
    public <T> List<T> shuffle(String sequence, List<T> values) {
        return delegate.shuffle(sequence, values);
    }

    private static UUID candidateId(Object candidate) {
        if (candidate instanceof BattlePersonage personage) {
            return personage.id();
        }
        if (candidate instanceof UUID uuid) {
            return uuid;
        }
        return null;
    }
}
