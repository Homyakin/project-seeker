package ru.homyakin.seeker.game.battle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.item.models.AttackType;

/**
 * Detailed in-memory trace used by deterministic battle verification.
 *
 * <p>These records deliberately do not implement {@link BattleEvent}: they are not part of the persisted battle
 * log or the replay protocol.</p>
 */
public sealed interface BattleTraceEvent permits
    BattleTraceEvent.TurnStarted,
    BattleTraceEvent.TurnFinished,
    BattleTraceEvent.NormalAttackAttempt {

    long turnId();

    UUID personageId();

    int ownTurn();

    int round();

    record TurnStarted(
        long turnId,
        UUID personageId,
        int ownTurn,
        int lineIndex,
        int round
    ) implements BattleTraceEvent {
        public TurnStarted {
            validateTurn(turnId, personageId, ownTurn, lineIndex, round);
        }
    }

    record TurnFinished(
        long turnId,
        UUID personageId,
        int ownTurn,
        int startLineIndex,
        int endLineIndex,
        boolean actionPerformed,
        boolean alive,
        int round
    ) implements BattleTraceEvent {
        public TurnFinished {
            validateTurn(turnId, personageId, ownTurn, startLineIndex, round);
            if (endLineIndex < 0) {
                throw new IllegalArgumentException("endLineIndex must be non-negative");
            }
            if (!actionPerformed && alive) {
                throw new IllegalArgumentException("a living personage cannot finish without an action");
            }
        }
    }

    record NormalAttackAttempt(
        long turnId,
        long attemptId,
        UUID attackerId,
        int ownTurn,
        UUID originalTargetId,
        UUID finalTargetId,
        AttackAccess access,
        int ordinaryRange,
        int distance,
        Map<AttackType, Integer> savedBasis,
        boolean critical,
        boolean dodged,
        int normalDamage,
        boolean targetDefeated,
        boolean discharge,
        Map<AttackType, Integer> dischargeBasis,
        int dischargeCoefficientNumerator,
        int dischargeCoefficientDenominator,
        int dischargeDamage,
        int accumulationChargesBefore,
        int accumulationChargesAfter,
        int lineBeforeRetreat,
        int lineAfterRetreat,
        int round
    ) implements BattleTraceEvent {
        public NormalAttackAttempt {
            validateTurn(turnId, attackerId, ownTurn, lineBeforeRetreat, round);
            if (attemptId <= 0) {
                throw new IllegalArgumentException("attemptId must be positive");
            }
            Objects.requireNonNull(originalTargetId, "originalTargetId");
            Objects.requireNonNull(finalTargetId, "finalTargetId");
            Objects.requireNonNull(access, "access");
            if (ordinaryRange <= 0 || distance <= 0) {
                throw new IllegalArgumentException("attack ranges must be positive");
            }
            savedBasis = immutableBasis(savedBasis, "savedBasis");
            dischargeBasis = immutableBasis(dischargeBasis, "dischargeBasis");
            if (normalDamage < 0 || dischargeDamage < 0) {
                throw new IllegalArgumentException("damage must be non-negative");
            }
            if (dodged && normalDamage != 0) {
                throw new IllegalArgumentException("a dodged attempt cannot deal normal damage");
            }
            if (!discharge && (!dischargeBasis.isEmpty()
                || dischargeCoefficientNumerator != 0
                || dischargeDamage != 0)) {
                throw new IllegalArgumentException("an ordinary attempt cannot contain discharge data");
            }
            if (discharge && (dischargeBasis.isEmpty() || dischargeCoefficientNumerator <= 0)) {
                throw new IllegalArgumentException("a discharge attempt must contain its basis and coefficient");
            }
            if (dischargeCoefficientNumerator < 0 || dischargeCoefficientDenominator <= 0) {
                throw new IllegalArgumentException("invalid discharge coefficient");
            }
            if (accumulationChargesBefore < 0 || accumulationChargesAfter < 0) {
                throw new IllegalArgumentException("accumulation charges must be non-negative");
            }
            if (lineAfterRetreat < 0) {
                throw new IllegalArgumentException("lineAfterRetreat must be non-negative");
            }
        }

        @Override
        public UUID personageId() {
            return attackerId;
        }
    }

    private static void validateTurn(long turnId, UUID personageId, int ownTurn, int lineIndex, int round) {
        if (turnId <= 0) {
            throw new IllegalArgumentException("turnId must be positive");
        }
        Objects.requireNonNull(personageId, "personageId");
        if (ownTurn <= 0) {
            throw new IllegalArgumentException("ownTurn must be positive");
        }
        if (lineIndex < 0 || round < 0) {
            throw new IllegalArgumentException("lineIndex and round must be non-negative");
        }
    }

    private static Map<AttackType, Integer> immutableBasis(Map<AttackType, Integer> basis, String name) {
        Objects.requireNonNull(basis, name);
        final var copy = new EnumMap<AttackType, Integer>(AttackType.class);
        for (final var entry : basis.entrySet()) {
            Objects.requireNonNull(entry.getKey(), name + " key");
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException(name + " values must be non-negative");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }
}
