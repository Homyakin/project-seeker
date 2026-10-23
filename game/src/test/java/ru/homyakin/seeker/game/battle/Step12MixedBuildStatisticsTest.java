package ru.homyakin.seeker.game.battle;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step12MixedBuildStatistics.Estimate;
import ru.homyakin.seeker.game.battle.Step12MixedBuildStatistics.Hypothesis;

class Step12MixedBuildStatisticsTest {
    @Test
    void pairedDifferenceKeepsValuesAtTheSameIteration() {
        final var estimate = Step12MixedBuildStatistics.pairedDifference(
            new double[] {3, 5, 7, 9},
            new double[] {1, 3, 5, 7}
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(2.0, estimate.point()),
            () -> Assertions.assertEquals(2.0, estimate.lower()),
            () -> Assertions.assertEquals(2.0, estimate.upper()),
            () -> Assertions.assertEquals(0.0, estimate.standardError()),
            () -> Assertions.assertEquals(0.0, estimate.pValue()),
            () -> Assertions.assertEquals(4, estimate.sampleSize()),
            () -> Assertions.assertEquals(0.0, estimate.sumSquaredDeviations())
        );
    }

    @Test
    void estimateUsesSampleErrorAndTwoSidedConfidenceInterval() {
        final var estimate = Step12MixedBuildStatistics.estimate(new double[] {1, 2, 3, 4});

        Assertions.assertAll(
            () -> Assertions.assertEquals(2.5, estimate.point()),
            () -> Assertions.assertEquals(Math.sqrt(5.0 / 12), estimate.standardError(), 1e-12),
            () -> Assertions.assertEquals(4, estimate.sampleSize()),
            () -> Assertions.assertEquals(5.0, estimate.sumSquaredDeviations()),
            () -> Assertions.assertTrue(estimate.lower() < estimate.point()),
            () -> Assertions.assertTrue(estimate.upper() > estimate.point()),
            () -> Assertions.assertTrue(estimate.pValue() > 0),
            () -> Assertions.assertTrue(estimate.pValue() < 0.05)
        );
    }

    @Test
    void holmIsMonotoneAndRestoresTheInputOrder() {
        final var adjusted = Step12MixedBuildStatistics.holm(List.of(
            hypothesis("FIRST", 0.01, 1),
            hypothesis("SECOND", 0.04, 1),
            hypothesis("THIRD", 0.03, 1)
        ));

        Assertions.assertAll(
            () -> Assertions.assertEquals(List.of("FIRST", "SECOND", "THIRD"),
                adjusted.stream().map(Step12MixedBuildStatistics.AdjustedHypothesis::code).toList()),
            () -> Assertions.assertEquals(0.03, adjusted.get(0).adjustedPValue(), 1e-12),
            () -> Assertions.assertEquals(0.06, adjusted.get(1).adjustedPValue(), 1e-12),
            () -> Assertions.assertEquals(0.06, adjusted.get(2).adjustedPValue(), 1e-12),
            () -> Assertions.assertTrue(adjusted.get(0).accepted()),
            () -> Assertions.assertFalse(adjusted.get(1).accepted()),
            () -> Assertions.assertFalse(adjusted.get(2).accepted())
        );
    }

    @Test
    void holmRequiresBothCorrectedSignificanceAndPositiveLowerBound() {
        final var adjusted = Step12MixedBuildStatistics.holm(List.of(
            hypothesis("POSITIVE", 0.001, 0.1),
            hypothesis("TOUCHES_ZERO", 0.001, 0)
        ));

        Assertions.assertAll(
            () -> Assertions.assertTrue(adjusted.get(0).accepted()),
            () -> Assertions.assertFalse(adjusted.get(1).accepted())
        );
    }

    @Test
    void rejectsInvalidSamplesPairsAndHypothesisFamilies() {
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.estimate(new double[0])
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.estimate(new double[] {1})
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.estimate(new double[] {1, Double.NaN})
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.pairedDifference(
                    new double[] {1, 2},
                    new double[] {1, 2, 3}
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.pairedDifference(
                    new double[] {Double.MAX_VALUE, Double.MAX_VALUE},
                    new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}
                )
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.holm(List.of())
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.holm(List.of(hypothesis(" ", 0.01, 1)))
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.holm(List.of(
                    hypothesis("DUPLICATE", 0.01, 1),
                    hypothesis("DUPLICATE", 0.02, 1)
                ))
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedBuildStatistics.holm(List.of(hypothesis("BAD_P", -0.01, 1)))
            )
        );
    }

    private static Hypothesis hypothesis(String code, double pValue, double lower) {
        return new Hypothesis(code, new Estimate(1, lower, 2, 0.1, pValue, 2, 0.02));
    }
}
