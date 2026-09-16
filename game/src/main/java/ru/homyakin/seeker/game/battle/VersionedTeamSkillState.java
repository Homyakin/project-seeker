package ru.homyakin.seeker.game.battle;

/** Shared runtime state of team-wide scalable mechanics. */
final class VersionedTeamSkillState {
    private int guardAttemptsRemaining;
    private boolean guardHighCooldownNext;

    /**
     * Advances an existing Guard cooldown for one enemy normal attempt.
     * An attempt that reduces the counter to zero still cannot be intercepted.
     */
    boolean guardReadyForAttempt() {
        if (guardAttemptsRemaining > 0) {
            guardAttemptsRemaining--;
            return false;
        }
        return true;
    }

    int consumeGuard(int lowerCooldown, int upperCooldown) {
        if (lowerCooldown < 0 || upperCooldown < lowerCooldown) {
            throw new IllegalArgumentException("invalid Guard cooldown bounds");
        }
        final int selected = guardHighCooldownNext ? upperCooldown : lowerCooldown;
        guardAttemptsRemaining = selected;
        guardHighCooldownNext = !guardHighCooldownNext;
        return selected;
    }

    int guardAttemptsRemaining() {
        return guardAttemptsRemaining;
    }
}
