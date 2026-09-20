package ru.homyakin.seeker.game.battle;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Step12CausalStatisticsTest {
    private static final long SEED = 2_026_092_001L;

    @Test
    void usesPreregisteredResampleCountAndPercentileRanks() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(20_000, Step12CausalStatistics.RESAMPLES),
            () -> Assertions.assertEquals(500, Step12CausalStatistics.LOWER_RANK),
            () -> Assertions.assertEquals(19_500, Step12CausalStatistics.UPPER_RANK),
            () -> Assertions.assertEquals(32, Step12CausalStatistics.MAX_FAST_MEAN_CATEGORIES)
        );
    }

    @Test
    void meanResamplesWholeObservationsDeterministically() {
        final var first = Step12CausalStatistics.mean(new double[] {0, 10}, SEED);
        final var second = Step12CausalStatistics.mean(new double[] {0, 10}, SEED);

        Assertions.assertAll(
            () -> Assertions.assertEquals(first, second),
            () -> Assertions.assertEquals(5.0, first.estimate()),
            () -> Assertions.assertEquals(0.0, first.confidenceInterval().lower()),
            () -> Assertions.assertEquals(10.0, first.confidenceInterval().upper()),
            () -> Assertions.assertEquals(10.0, first.confidenceInterval().width())
        );
    }

    @Test
    void categoricalMeanFastPathMatchesDirectBootstrapDistribution() {
        final var observations = new double[100];
        for (int index = 0; index < observations.length; index++) {
            observations[index] = index < 30 ? 0 : index < 70 ? 0.5 : 1;
        }
        final var fast = Step12CausalStatistics.mean(observations, SEED);
        final var repeated = Step12CausalStatistics.mean(observations, SEED);
        final var direct = directMean(observations, SEED);

        Assertions.assertAll(
            () -> Assertions.assertEquals(fast, repeated),
            () -> Assertions.assertEquals(direct.estimate(), fast.estimate()),
            () -> Assertions.assertEquals(
                direct.confidenceInterval().lower(),
                fast.confidenceInterval().lower(),
                0.02
            ),
            () -> Assertions.assertEquals(
                direct.confidenceInterval().upper(),
                fast.confidenceInterval().upper(),
                0.02
            )
        );
    }

    @Test
    void halfUnitMeanFastPathSupportsNegativeInteractionContrasts() {
        final var observations = new double[90];
        final var values = new double[] {-2, -1.5, -1, -0.5, 0, 0.5, 1, 1.5, 2};
        for (int index = 0; index < observations.length; index++) {
            observations[index] = values[index % values.length];
        }
        observations[0] = 2;
        observations[1] = 1.5;
        final var fast = Step12CausalStatistics.mean(observations, SEED);
        final var repeated = Step12CausalStatistics.mean(observations, SEED);
        final var direct = directMean(observations, SEED);

        Assertions.assertAll(
            () -> Assertions.assertEquals(fast, repeated),
            () -> Assertions.assertEquals(direct.estimate(), fast.estimate()),
            () -> Assertions.assertEquals(
                direct.confidenceInterval().lower(),
                fast.confidenceInterval().lower(),
                0.08
            ),
            () -> Assertions.assertEquals(
                direct.confidenceInterval().upper(),
                fast.confidenceInterval().upper(),
                0.08
            )
        );
    }

    @Test
    void nonHalfUnitMeanRetainsDeterministicGenericPath() {
        final var observations = new double[] {-0.25, 0.25, 0.75, 1.25};
        final var first = Step12CausalStatistics.mean(observations, SEED);
        final var second = Step12CausalStatistics.mean(observations, SEED);

        Assertions.assertEquals(first, second);
    }

    @Test
    void tooManyHalfUnitCategoriesUseTheUnboundedGenericPath() {
        final var observations = new double[33];
        for (int index = 0; index < observations.length; index++) {
            observations[index] = (index - 16) / 2.0;
        }
        final var actual = Step12CausalStatistics.mean(observations, SEED);
        final var repeated = Step12CausalStatistics.mean(observations, SEED);
        final var direct = directMean(observations, SEED);

        Assertions.assertAll(
            () -> Assertions.assertEquals(direct, actual),
            () -> Assertions.assertEquals(actual, repeated)
        );
    }

    @Test
    void pairedDifferenceKeepsBothValuesAtTheSameSeedIndex() {
        final var result = Step12CausalStatistics.pairedDifference(
            new double[] {3, 5},
            new double[] {1, 1},
            SEED
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(3.0, result.estimate()),
            () -> Assertions.assertEquals(2.0, result.confidenceInterval().lower()),
            () -> Assertions.assertEquals(4.0, result.confidenceInterval().upper())
        );
    }

    @Test
    void ratioIsRecomputedAsRatioOfSumsInEveryResample() {
        final var constant = Step12CausalStatistics.ratioOfSums(
            new double[] {2, 6},
            new double[] {1, 3},
            SEED
        );
        final var weighted = Step12CausalStatistics.ratioOfSums(
            new double[] {1, 9},
            new double[] {1, 3},
            SEED
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(2.0, constant.estimate()),
            () -> Assertions.assertEquals(2.0, constant.confidenceInterval().lower()),
            () -> Assertions.assertEquals(2.0, constant.confidenceInterval().upper()),
            () -> Assertions.assertEquals(2.5, weighted.estimate()),
            () -> Assertions.assertNotEquals(2.0, weighted.estimate()),
            () -> Assertions.assertEquals(1.0, weighted.confidenceInterval().lower()),
            () -> Assertions.assertEquals(3.0, weighted.confidenceInterval().upper())
        );
    }

    @Test
    void ratioOfRatiosRecalculatesAllFourSumsInEveryResample() {
        final var constant = Step12CausalStatistics.ratioOfRatios(
            new double[] {4, 12},
            new double[] {2, 6},
            new double[] {2, 6},
            new double[] {2, 6},
            SEED
        );
        final var weighted = Step12CausalStatistics.ratioOfRatios(
            new double[] {1, 9},
            new double[] {1, 3},
            new double[] {2, 2},
            new double[] {1, 1},
            SEED
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(2.0, constant.estimate()),
            () -> Assertions.assertEquals(2.0, constant.confidenceInterval().lower()),
            () -> Assertions.assertEquals(2.0, constant.confidenceInterval().upper()),
            () -> Assertions.assertEquals(1.25, weighted.estimate()),
            () -> Assertions.assertEquals(0.5, weighted.confidenceInterval().lower()),
            () -> Assertions.assertEquals(1.5, weighted.confidenceInterval().upper())
        );
    }

    @Test
    void signFlipPValueIsDeterministicAndUsesPairedDifferences() {
        final var first = new double[] {4, 5, 6, 7, 8, 9, 10, 11};
        final var second = new double[] {0, 0, 0, 0, 0, 0, 0, 0};
        final double fromPairs = Step12CausalStatistics.pairedSignFlipPValue(first, second, SEED);
        final double fromDifferences = Step12CausalStatistics.pairedSignFlipPValue(first, SEED);

        Assertions.assertAll(
            () -> Assertions.assertEquals(fromPairs, fromDifferences),
            () -> Assertions.assertTrue(fromPairs > 0),
            () -> Assertions.assertTrue(fromPairs < 0.02),
            () -> Assertions.assertEquals(
                Math.rint(fromPairs * (Step12CausalStatistics.RESAMPLES + 1) - 1),
                fromPairs * (Step12CausalStatistics.RESAMPLES + 1) - 1,
                1e-9
            )
        );
    }

    @Test
    void halfUnitSignFlipFastPathMatchesDirectInteractionContrastDistribution() {
        final var differences = new double[] {
            2, 2, 1.5, 1, 0.5, 0, -0.5, -1, -1.5, -2,
            2, 1.5, 0.5, 0, -0.5, -1.5,
        };
        final double fast = Step12CausalStatistics.pairedSignFlipPValue(differences, SEED);
        final double repeated = Step12CausalStatistics.pairedSignFlipPValue(differences, SEED);
        final double direct = directSignFlipPValue(differences, SEED);

        Assertions.assertAll(
            () -> Assertions.assertEquals(fast, repeated),
            () -> Assertions.assertEquals(direct, fast, 0.02)
        );
    }

    @Test
    void nonHalfUnitSignFlipRetainsDeterministicGenericPath() {
        final var differences = new double[] {0.25, 0.75, 1.25, -0.25, 0.25};
        final double first = Step12CausalStatistics.pairedSignFlipPValue(differences, SEED);
        final double second = Step12CausalStatistics.pairedSignFlipPValue(differences, SEED);

        Assertions.assertEquals(first, second);
    }

    @Test
    void signFlipReturnsOneWhenEveryPermutationIsAsExtreme() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                1.0,
                Step12CausalStatistics.pairedSignFlipPValue(new double[] {0, 0, 0}, SEED)
            ),
            () -> Assertions.assertEquals(
                1.0,
                Step12CausalStatistics.pairedSignFlipPValue(new double[] {5}, SEED)
            )
        );
    }

    @Test
    void holmCorrectionIsMonotoneInSortedOrderAndRestoresInputOrder() {
        final var adjusted = Step12CausalStatistics.holmAdjustedPValues(
            new double[] {0.01, 0.04, 0.03}
        );

        Assertions.assertArrayEquals(new double[] {0.03, 0.06, 0.06}, adjusted, 1e-12);
    }

    @Test
    void rejectsInvalidSamplesAndPValues() {
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CausalStatistics.mean(new double[0], SEED)
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CausalStatistics.pairedDifference(
                    new double[] {1},
                    new double[] {1, 2},
                    SEED
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CausalStatistics.ratioOfSums(
                    new double[] {1, 2},
                    new double[] {0, 0},
                    SEED
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CausalStatistics.ratioOfRatios(
                    new double[] {1},
                    new double[] {1},
                    new double[] {0},
                    new double[] {1},
                    SEED
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CausalStatistics.mean(new double[] {Double.NaN}, SEED)
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12CausalStatistics.holmAdjustedPValues(new double[] {-0.1, 0.2})
            ),
            () -> Assertions.assertArrayEquals(
                new double[0],
                Step12CausalStatistics.holmAdjustedPValues(new double[0])
            )
        );
    }

    private static Step12CausalStatistics.Estimate directMean(double[] observations, long seed) {
        final var random = new SplittableRandom(seed);
        final var estimates = new double[Step12CausalStatistics.RESAMPLES];
        for (int resample = 0; resample < estimates.length; resample++) {
            var sum = 0.0;
            for (int draw = 0; draw < observations.length; draw++) {
                sum += observations[random.nextInt(observations.length)];
            }
            estimates[resample] = sum / observations.length;
        }
        java.util.Arrays.sort(estimates);
        return new Step12CausalStatistics.Estimate(
            java.util.Arrays.stream(observations).average().orElseThrow(),
            new Step12CausalStatistics.ConfidenceInterval(
                estimates[Step12CausalStatistics.LOWER_RANK - 1],
                estimates[Step12CausalStatistics.UPPER_RANK - 1]
            )
        );
    }

    private static double directSignFlipPValue(double[] differences, long seed) {
        final double observed = Math.abs(java.util.Arrays.stream(differences).average().orElseThrow());
        final var random = new SplittableRandom(seed);
        var count = 0;
        for (int resample = 0; resample < Step12CausalStatistics.RESAMPLES; resample++) {
            var sum = 0.0;
            for (final double difference : differences) {
                sum += random.nextBoolean() ? difference : -difference;
            }
            if (Math.abs(sum / differences.length) >= observed) {
                count++;
            }
        }
        return (1.0 + count) / (Step12CausalStatistics.RESAMPLES + 1.0);
    }
}
