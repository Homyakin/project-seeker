package ru.homyakin.seeker.game.event.pve;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PveCharacteristicScalingTest {
    @Test
    void minimumPowerAboveTargetIsAnExplicitResult() {
        final var result = PveCharacteristicScaling.search(0, 100, multiplier -> 7 + multiplier);

        Assertions.assertEquals(0, result.multiplier());
        Assertions.assertEquals(7, result.achievedPower());
        Assertions.assertEquals(0, result.targetPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, result.status());
        Assertions.assertEquals(1, result.evaluations());
    }

    @Test
    void targetEqualToMinimumDoesNotNeedAnotherEvaluation() {
        final var result = PveCharacteristicScaling.search(7, 100, multiplier -> 7 + multiplier);

        Assertions.assertEquals(0, result.multiplier());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MATCHED, result.status());
        Assertions.assertEquals(1, result.evaluations());
    }

    @Test
    void selectsTheClosestDiscreteLowerStepRatherThanDemandingRelativeTolerance() {
        final var result = PveCharacteristicScaling.search(
            12,
            100,
            multiplier -> 10 * PveCharacteristicScaling.multiply(1, multiplier)
        );

        Assertions.assertEquals(10, result.achievedPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, result.status());
        Assertions.assertEquals(1, PveCharacteristicScaling.multiply(1, result.multiplier()));
    }

    @Test
    void selectsTheClosestUpperStepAndUsesTheUpperStepForTies() {
        for (final var target : new double[]{15, 18}) {
            final var result = PveCharacteristicScaling.search(
                target,
                100,
                multiplier -> 10 * PveCharacteristicScaling.multiply(1, multiplier)
            );

            Assertions.assertEquals(20, result.achievedPower());
            Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, result.status());
        }
    }

    @Test
    void exactReachablePowerReturnsMatched() {
        final var result = PveCharacteristicScaling.search(16, 100, multiplier -> multiplier * multiplier);

        Assertions.assertEquals(4, result.multiplier());
        Assertions.assertEquals(16, result.achievedPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MATCHED, result.status());
    }

    @Test
    void smallestPositiveMultiplierCanBeMatchedWithoutUnderflowingOrEvaluatingPastTheLimit() {
        final var result = PveCharacteristicScaling.search(Double.MIN_VALUE, Double.MIN_VALUE, multiplier -> {
            Assertions.assertTrue(multiplier <= Double.MIN_VALUE);
            return multiplier;
        });

        Assertions.assertEquals(Double.MIN_VALUE, result.multiplier());
        Assertions.assertEquals(Double.MIN_VALUE, result.achievedPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.MATCHED, result.status());
        Assertions.assertEquals(2, result.evaluations());
    }

    @Test
    void subnormalIntervalWithoutARepresentableMiddleChoosesTheClosestEndpoint() {
        final var result = PveCharacteristicScaling.search(
            0.5,
            Double.MIN_VALUE,
            multiplier -> multiplier == 0 ? 0 : 1
        );

        Assertions.assertEquals(Double.MIN_VALUE, result.multiplier());
        Assertions.assertEquals(1, result.achievedPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, result.status());
        Assertions.assertEquals(2, result.evaluations());
    }

    @Test
    void tinyUnreachableIntervalFailsAfterEvaluatingOnlyItsEndpoints() {
        final var calls = new AtomicInteger();
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.search(
            2e-300,
            1e-300,
            multiplier -> {
                calls.incrementAndGet();
                Assertions.assertTrue(multiplier <= 1e-300);
                return multiplier;
            }
        ));

        Assertions.assertEquals(2, calls.get());
    }

    @Test
    void retainsAdjacentDiscreteStepsAtALargeMultiplier() {
        final var result = PveCharacteristicScaling.search(
            120_000_000.25,
            PveCharacteristicScaling.maxMultiplier(1),
            multiplier -> PveCharacteristicScaling.multiply(1, multiplier)
        );

        Assertions.assertEquals(120_000_000, result.achievedPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, result.status());
        Assertions.assertTrue(result.evaluations() <= 100);
    }

    @Test
    void reportsTheNumberOfEvaluationsWithoutReevaluatingChosenEndpoints() {
        final var calls = new AtomicInteger();
        final var result = PveCharacteristicScaling.search(13, 100, multiplier -> {
            calls.incrementAndGet();
            return 10 * PveCharacteristicScaling.multiply(1, multiplier);
        });

        Assertions.assertEquals(calls.get(), result.evaluations());
        Assertions.assertTrue(result.evaluations() <= 68);
    }

    @Test
    void unreachableTargetStopsAtTheSuppliedLimitWithoutOvershooting() {
        final var calls = new AtomicInteger();
        final var error = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PveCharacteristicScaling.search(100, 3, multiplier -> {
                calls.incrementAndGet();
                Assertions.assertTrue(multiplier <= 3);
                return multiplier;
            })
        );

        Assertions.assertTrue(error.getMessage().contains("unreachable"));
        Assertions.assertEquals(4, calls.get());
    }

    @Test
    void evenTheLargestFiniteSearchIntervalHasABoundedExpansionCount() {
        final var calls = new AtomicInteger();
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.search(
            1,
            Double.MAX_VALUE,
            multiplier -> {
                calls.incrementAndGet();
                Assertions.assertTrue(Double.isFinite(multiplier));
                return 0;
            }
        ));

        Assertions.assertTrue(calls.get() <= 1026);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidTargetBeforeCallingTheEvaluator(double target) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.search(
            target,
            1,
            multiplier -> Assertions.fail("An invalid target must not invoke the evaluator")
        ));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidSearchLimit(double limit) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.search(1, limit, value -> value));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidEvaluatedPowerAtTheMinimumAndDuringExpansion(double evaluatedPower) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PveCharacteristicScaling.search(1, 10, multiplier -> evaluatedPower)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.search(
            1,
            10,
            multiplier -> multiplier == 0 ? 0 : evaluatedPower
        ));
    }

    @Test
    void preservesCeilAndMinimumOneWithoutSilentlySaturating() {
        Assertions.assertEquals(1, PveCharacteristicScaling.multiply(100, 0));
        Assertions.assertEquals(1, PveCharacteristicScaling.multiply(100, 0.001));
        Assertions.assertEquals(101, PveCharacteristicScaling.multiply(100, 1.001));
        Assertions.assertEquals(
            PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC,
            PveCharacteristicScaling.multiply(PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC, 1)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.multiply(Integer.MAX_VALUE, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.multiply(1, Double.MAX_VALUE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.multiply(Integer.MAX_VALUE, Double.MAX_VALUE));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidMultipliers(double multiplier) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.multiply(100, multiplier));
    }

    @Test
    void safeLimitUsesEveryBaseAndAccountsForFloatingPointRounding() {
        final var bases = new int[]{70, 130, 420, 280, 2000, Integer.MAX_VALUE};
        final var maximum = PveCharacteristicScaling.maxMultiplier(bases);

        Assertions.assertTrue(Double.isFinite(maximum));
        Assertions.assertTrue(maximum > 0);
        Assertions.assertTrue(maximum < 1);
        for (final var base : bases) {
            Assertions.assertTrue(PveCharacteristicScaling.multiply(base, maximum) <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC);
        }
    }

    @Test
    void rejectsMissingOrNonPositiveBaseCharacteristics() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.maxMultiplier());
        for (final var base : new int[]{0, -1, Integer.MIN_VALUE}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.multiply(base, 0));
            Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.maxMultiplier(100, base));
        }
    }

    @Test
    void weightedScalingHasOneCeilAfterTheSpecifiedTwoMultiplications() {
        Assertions.assertEquals(2, PveCharacteristicScaling.multiply(3, 0.4, 1.5));
        Assertions.assertEquals(1, PveCharacteristicScaling.multiply(3, 0.6, 0.5));
        Assertions.assertEquals(13, PveCharacteristicScaling.multiply(70, 13.0 / 105, 1.5));
        Assertions.assertEquals(12, PveCharacteristicScaling.multiply(7, 11.0 / 10.5, 1.5));
        Assertions.assertEquals(14, (int) Math.ceil((70 * (13.0 / 105)) * 1.5));
        Assertions.assertEquals(11, (int) Math.ceil((7 * (11.0 / 10.5)) * 1.5));
    }

    @Test
    void weightedScalingPreservesMinimumAndTheUnweightedPathWhenWeightIsOne() {
        for (final var weight : new double[]{Double.MIN_VALUE, 0.75, 1, 1.5, Double.MAX_VALUE}) {
            Assertions.assertEquals(1, PveCharacteristicScaling.multiply(420, 0, weight));
        }
        Assertions.assertEquals(1, PveCharacteristicScaling.multiply(420, Double.MIN_VALUE, Double.MIN_VALUE));
        for (final var base : new int[]{1, 70, 130, 280, 420, PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC}) {
            for (final var multiplier : new double[]{0, 0.001, 0.5, 1}) {
                Assertions.assertEquals(
                    PveCharacteristicScaling.multiply(base, multiplier),
                    PveCharacteristicScaling.multiply(base, multiplier, 1)
                );
            }
        }
    }

    @Test
    void weightedScalingRejectsOverflowInsteadOfSaturating() {
        final var limit = PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC;
        Assertions.assertEquals(limit, PveCharacteristicScaling.multiply(limit, 1, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveCharacteristicScaling.multiply(1, Math.nextUp((double) limit), 1));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveCharacteristicScaling.multiply(420, Double.MAX_VALUE, 1.5));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveCharacteristicScaling.multiply(420, 2, Double.MAX_VALUE));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void weightedScalingRejectsInvalidMultipliers(double multiplier) {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveCharacteristicScaling.multiply(420, multiplier, 1.5));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -0.0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidRoleWeightsEvenAtZeroMultiplier(double weight) {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveCharacteristicScaling.multiply(420, 0, weight));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> new PveCharacteristicScaling.WeightedBase(420, weight));
    }

    @Test
    void weightedBoundsValidateTheBaseAndRequireAllDeclaredCharacteristics() {
        Assertions.assertThrows(NullPointerException.class,
            () -> PveCharacteristicScaling.maxWeightedMultiplier((PveCharacteristicScaling.WeightedBase[]) null));
        Assertions.assertThrows(IllegalArgumentException.class, PveCharacteristicScaling::maxWeightedMultiplier);
        Assertions.assertThrows(NullPointerException.class,
            () -> PveCharacteristicScaling.maxWeightedMultiplier((PveCharacteristicScaling.WeightedBase) null));
        for (final var base : new int[]{0, -1, Integer.MIN_VALUE}) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> PveCharacteristicScaling.multiply(base, 0, 1.5));
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PveCharacteristicScaling.WeightedBase(base, 1.5));
        }
    }

    @Test
    void safeWeightedBoundAccountsForTheRoundingOfBothMultiplications() {
        final int base = 149;
        final double weight = 11.0 / 12;
        final var unsafeCollapsedBound = Math.nextDown(
            PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC / (base * weight)
        );
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveCharacteristicScaling.multiply(base, unsafeCollapsedBound, weight));

        final var safe = PveCharacteristicScaling.maxWeightedMultiplier(
            new PveCharacteristicScaling.WeightedBase(base, weight)
        );
        Assertions.assertTrue(safe < unsafeCollapsedBound);
        Assertions.assertTrue(PveCharacteristicScaling.multiply(base, safe, weight)
            <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC);
    }

    @Test
    void weightedBoundIsSafeAcrossNpcBasesAndVerySmallAndLargeWeights() {
        for (final var base : new int[]{1, 7, 70, 130, 149, 160, 280, 420, 1999, Integer.MAX_VALUE}) {
            for (final var weight : new double[]{
                Double.MIN_VALUE, Double.MIN_NORMAL, 1e-300, 0.75, 11.0 / 12,
                Math.nextDown(1.0), 1, Math.nextUp(1.0), 16.5 / 16, 1.5, 16, 16.5, 1e300, Double.MAX_VALUE
            }) {
                final var bound = PveCharacteristicScaling.maxWeightedMultiplier(
                    new PveCharacteristicScaling.WeightedBase(base, weight)
                );
                Assertions.assertTrue(Double.isFinite(bound));
                Assertions.assertTrue(bound > 0);
                Assertions.assertTrue(PveCharacteristicScaling.multiply(base, bound, weight)
                    <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC);
            }
        }
        for (int count = 2; count <= 1000; ++count) {
            final int front = count / 2;
            final int back = count - front;
            final var bases = new PveCharacteristicScaling.WeightedBase[]{
                new PveCharacteristicScaling.WeightedBase(70, count / (2.0 * front)),
                new PveCharacteristicScaling.WeightedBase(420, count / (2.0 * front)),
                new PveCharacteristicScaling.WeightedBase(160, 1),
                new PveCharacteristicScaling.WeightedBase(130, count / (2.0 * back)),
                new PveCharacteristicScaling.WeightedBase(280, count / (2.0 * back)),
                new PveCharacteristicScaling.WeightedBase(70, 1)
            };
            final var bound = PveCharacteristicScaling.maxWeightedMultiplier(bases);
            for (final var weightedBase : bases) {
                Assertions.assertTrue(PveCharacteristicScaling.multiply(weightedBase.base(), bound, weightedBase.weight())
                    <= PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC);
            }
        }
    }

    @Test
    void weightedBoundIsSafeForAllBasesNotJustTheFirstAndSearchRetainsItsLimit() {
        final var bases = new PveCharacteristicScaling.WeightedBase[]{
            new PveCharacteristicScaling.WeightedBase(70, 1),
            new PveCharacteristicScaling.WeightedBase(420, 1.5),
            new PveCharacteristicScaling.WeightedBase(160, 1)
        };
        final var maximum = PveCharacteristicScaling.maxWeightedMultiplier(bases);
        Assertions.assertTrue(maximum < PveCharacteristicScaling.maxWeightedMultiplier(bases[0]));
        final var result = PveCharacteristicScaling.search(100_000_000.25, maximum,
            multiplier -> PveCharacteristicScaling.multiply(420, multiplier, 1.5));
        Assertions.assertEquals(100_000_000, result.achievedPower());
        Assertions.assertEquals(PveCharacteristicScaling.Status.CLOSEST_STEP, result.status());
        Assertions.assertTrue(result.multiplier() <= maximum);
        Assertions.assertTrue(result.evaluations() <= 100);
        final var calls = new AtomicInteger();
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.search(
            PveCharacteristicScaling.MAX_SCALED_CHARACTERISTIC + 1.0,
            maximum,
            multiplier -> {
                calls.incrementAndGet();
                Assertions.assertTrue(multiplier <= maximum);
                return PveCharacteristicScaling.multiply(420, multiplier, 1.5);
            }
        ));
        Assertions.assertTrue(calls.get() <= 100);
    }

    @Test
    void aTinyWeightHasAFiniteBoundAndAnExplicitUnreachableTarget() {
        final var bound = PveCharacteristicScaling.maxWeightedMultiplier(
            new PveCharacteristicScaling.WeightedBase(1, Double.MIN_VALUE)
        );
        Assertions.assertEquals(Double.MAX_VALUE, bound);
        Assertions.assertEquals(1, PveCharacteristicScaling.multiply(1, bound, Double.MIN_VALUE));
        final var calls = new AtomicInteger();
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveCharacteristicScaling.search(2, bound, multiplier -> {
            calls.incrementAndGet();
            Assertions.assertTrue(Double.isFinite(multiplier));
            return PveCharacteristicScaling.multiply(1, multiplier, Double.MIN_VALUE);
        }));
        Assertions.assertTrue(calls.get() <= 1026);
        final var minimum = PveCharacteristicScaling.search(0, bound,
            multiplier -> PveCharacteristicScaling.multiply(1, multiplier, Double.MIN_VALUE));
        Assertions.assertEquals(PveCharacteristicScaling.Status.MINIMUM_EXCEEDS_TARGET, minimum.status());
        Assertions.assertEquals(1, minimum.evaluations());
    }
}
