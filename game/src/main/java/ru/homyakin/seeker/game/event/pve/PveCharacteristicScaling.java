package ru.homyakin.seeker.game.event.pve;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/** Bounded calibration of the common multiplier of synthetic PvE enemy characteristics. */
public final class PveCharacteristicScaling {
    /**
     * Arithmetic headroom for the current single-item NPC factories, including critical damage,
     * their four-point skills and accelerated battle tempo. This is not a universal bound for
     * arbitrary player builds, stacked effects or future NPC skills.
     */
    public static final int MAX_SCALED_CHARACTERISTIC = Integer.MAX_VALUE / 16;
    private static final int MAX_BRACKETING_STEPS = 1024;
    private static final int MAX_BISECTION_STEPS = 64;

    private PveCharacteristicScaling() {
    }

    /**
     * Finds the closest of the two attainable endpoint powers around the target. The callback
     * must be deterministic and nondecreasing on the supplied multiplier interval. Integer
     * characteristic steps need not meet an arbitrary relative-error tolerance; ties choose
     * the upper endpoint, as in the historical enemy generators.
     */
    public static Result search(double targetPower, double maxMultiplier, DoubleUnaryOperator power) {
        requireFiniteNonNegative(targetPower, "Target power");
        if (!Double.isFinite(maxMultiplier) || maxMultiplier <= 0) {
            throw new IllegalArgumentException("Maximum multiplier must be positive and finite: " + maxMultiplier);
        }
        Objects.requireNonNull(power, "power");

        var evaluations = 1;
        var low = 0.0;
        var lowPower = evaluate(power, low);
        if (lowPower >= targetPower) {
            return new Result(
                low,
                targetPower,
                lowPower,
                lowPower == targetPower ? Status.MATCHED : Status.MINIMUM_EXCEEDS_TARGET,
                evaluations
            );
        }

        var high = Math.min(1.0, maxMultiplier);
        var highPower = evaluate(power, high);
        ++evaluations;
        var expansions = 0;
        while (highPower < targetPower) {
            if (high >= maxMultiplier || expansions >= MAX_BRACKETING_STEPS) {
                throw new IllegalArgumentException(
                    "Target power %s is unreachable within multiplier %s (achieved %s after %s evaluations)"
                        .formatted(targetPower, maxMultiplier, highPower, evaluations)
                );
            }
            low = high;
            lowPower = highPower;
            high = Math.min(maxMultiplier, high * 2.0);
            highPower = evaluate(power, high);
            ++evaluations;
            ++expansions;
        }

        if (highPower == targetPower) {
            return new Result(high, targetPower, highPower, Status.MATCHED, evaluations);
        }

        for (var step = 0; step < MAX_BISECTION_STEPS; ++step) {
            final var middle = low + (high - low) / 2.0;
            if (middle <= low || middle >= high) {
                break;
            }
            final var middlePower = evaluate(power, middle);
            ++evaluations;
            if (middlePower == targetPower) {
                return new Result(middle, targetPower, middlePower, Status.MATCHED, evaluations);
            }
            if (middlePower >= targetPower) {
                high = middle;
                highPower = middlePower;
            } else {
                low = middle;
                lowPower = middlePower;
            }
        }

        if (targetPower - lowPower < highPower - targetPower) {
            return new Result(low, targetPower, lowPower, Status.CLOSEST_STEP, evaluations);
        }
        return new Result(high, targetPower, highPower, Status.CLOSEST_STEP, evaluations);
    }

    /** Scales a positive base with the historical ceil/minimum-one rule, without narrowing saturation. */
    public static int multiply(int base, double multiplier) {
        requirePositiveBase(base);
        requireFiniteNonNegative(multiplier, "Characteristic multiplier");
        final var scaled = Math.ceil(base * multiplier);
        if (!Double.isFinite(scaled) || scaled > MAX_SCALED_CHARACTERISTIC) {
            throw new IllegalArgumentException(
                "Scaled characteristic exceeds the safe NPC limit %s: base=%s, multiplier=%s"
                    .formatted(MAX_SCALED_CHARACTERISTIC, base, multiplier)
            );
        }
        return Math.max(1, (int) scaled);
    }

    /** Applies the role weight before scaling the base, with exactly one final ceil. */
    public static int multiply(int base, double multiplier, double weight) {
        requirePositiveBase(base);
        requireFiniteNonNegative(multiplier, "Characteristic multiplier");
        requireFinitePositive(weight, "Characteristic weight");
        return multiply(base, multiplier * weight);
    }

    /** Safe common upper multiplier for all scaled bases in every enemy kind of one generator. */
    public static double maxMultiplier(int... bases) {
        Objects.requireNonNull(bases, "bases");
        if (bases.length == 0) {
            throw new IllegalArgumentException("At least one positive base characteristic is required");
        }
        var largestBase = 0;
        for (final var base : bases) {
            requirePositiveBase(base);
            largestBase = Math.max(largestBase, base);
        }
        // Round down so ceil(base * multiplier) cannot exceed the limit due to division rounding.
        return Math.nextDown((double) MAX_SCALED_CHARACTERISTIC / largestBase);
    }

    /**
     * Safe common upper multiplier for the actual evaluation order {@code base * (m * weight)}.
     * Each inverse operation rounds down before the next division. Therefore the rounded inner
     * product cannot exceed its safe limit, and the rounded outer product cannot exceed the
     * representable integer characteristic limit. A precomputed {@code base * weight} would not
     * establish that guarantee for the two separately rounded multiplications.
     */
    public static double maxWeightedMultiplier(WeightedBase... bases) {
        Objects.requireNonNull(bases, "bases");
        if (bases.length == 0) {
            throw new IllegalArgumentException("At least one positive weighted base is required");
        }
        var maximum = Double.MAX_VALUE;
        for (final var weightedBase : bases) {
            Objects.requireNonNull(weightedBase, "weightedBase");
            final var innerLimit = Math.nextDown((double) MAX_SCALED_CHARACTERISTIC / weightedBase.base());
            final var quotient = innerLimit / weightedBase.weight();
            final var candidate = Double.isFinite(quotient) ? Math.nextDown(quotient) : Double.MAX_VALUE;
            maximum = Math.min(maximum, candidate);
        }
        requireFinitePositive(maximum, "Maximum weighted multiplier");
        // Validate the exact production expression as well as the conservative inverse bound.
        for (final var weightedBase : bases) {
            multiply(weightedBase.base(), maximum, weightedBase.weight());
        }
        return maximum;
    }

    private static double evaluate(DoubleUnaryOperator power, double multiplier) {
        final var result = power.applyAsDouble(multiplier);
        requireFiniteNonNegative(result, "Evaluated enemy power at multiplier " + multiplier);
        return result;
    }

    private static void requirePositiveBase(int base) {
        if (base <= 0) {
            throw new IllegalArgumentException("Base characteristic must be positive: " + base);
        }
    }

    private static void requireFiniteNonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(label + " must be finite and non-negative: " + value);
        }
    }

    private static void requireFinitePositive(double value, String label) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(label + " must be finite and positive: " + value);
        }
    }

    public record WeightedBase(int base, double weight) {
        public WeightedBase {
            requirePositiveBase(base);
            requireFinitePositive(weight, "Characteristic weight");
        }
    }

    public enum Status {
        MATCHED,
        CLOSEST_STEP,
        MINIMUM_EXCEEDS_TARGET
    }

    public record Result(
        double multiplier,
        double targetPower,
        double achievedPower,
        Status status,
        int evaluations
    ) {
        public Result {
            requireFiniteNonNegative(multiplier, "Selected multiplier");
            requireFiniteNonNegative(targetPower, "Target power");
            requireFiniteNonNegative(achievedPower, "Achieved power");
            Objects.requireNonNull(status, "status");
            if (evaluations <= 0) {
                throw new IllegalArgumentException("Evaluation count must be positive: " + evaluations);
            }
        }
    }
}
