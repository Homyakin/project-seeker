package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.apache.commons.math3.distribution.TDistribution;

/** Парные оценки и единая поправка Холма для слоя смешанных сборок. */
final class Step12MixedBuildStatistics {
    private static final double FAMILY_ALPHA = 0.05;

    private Step12MixedBuildStatistics() {
    }

    static Estimate pairedDifference(double[] minuend, double[] subtrahend) {
        validatePair(minuend, subtrahend);
        final var differences = new double[minuend.length];
        for (int index = 0; index < differences.length; index++) {
            differences[index] = finite(minuend[index] - subtrahend[index], "paired difference");
        }
        return estimate(differences);
    }

    static Estimate estimate(double[] observations) {
        validateObservations(observations);
        final double mean = Arrays.stream(observations).average().orElseThrow();
        var squaredDeviations = 0.0;
        for (final double observation : observations) {
            final double deviation = observation - mean;
            squaredDeviations += deviation * deviation;
        }
        final double standardDeviation = observations.length == 1
            ? 0
            : Math.sqrt(squaredDeviations / (observations.length - 1));
        final double standardError = standardDeviation / Math.sqrt(observations.length);
        if (standardError == 0) {
            final double pValue = mean == 0 ? 1 : 0;
            return new Estimate(
                mean,
                mean,
                mean,
                standardError,
                pValue,
                observations.length,
                squaredDeviations
            );
        }
        final var distribution = new TDistribution(observations.length - 1.0);
        final double critical = distribution.inverseCumulativeProbability(0.975);
        final double statistic = Math.abs(mean / standardError);
        final double pValue = Math.max(0, Math.min(1, 2 * (1 - distribution.cumulativeProbability(statistic))));
        return new Estimate(
            mean,
            mean - critical * standardError,
            mean + critical * standardError,
            standardError,
            pValue,
            observations.length,
            squaredDeviations
        );
    }

    static List<AdjustedHypothesis> holm(List<Hypothesis> hypotheses) {
        Objects.requireNonNull(hypotheses, "hypotheses");
        if (hypotheses.isEmpty()) {
            throw new IllegalArgumentException("At least one hypothesis is required");
        }
        final var order = new ArrayList<Integer>(hypotheses.size());
        final var codes = new HashSet<String>();
        for (int index = 0; index < hypotheses.size(); index++) {
            final var hypothesis = Objects.requireNonNull(hypotheses.get(index), "hypothesis");
            if (hypothesis.code().isBlank()) {
                throw new IllegalArgumentException("Hypothesis code must not be blank");
            }
            if (!codes.add(hypothesis.code())) {
                throw new IllegalArgumentException("Hypothesis code must be unique: " + hypothesis.code());
            }
            validateEstimate(hypothesis.estimate());
            order.add(index);
        }
        order.sort(Comparator
            .comparingDouble((Integer index) -> hypotheses.get(index).estimate().pValue())
            .thenComparingInt(Integer::intValue));

        final var adjusted = new double[hypotheses.size()];
        var previous = 0.0;
        for (int rank = 0; rank < order.size(); rank++) {
            final int index = order.get(rank);
            final double scaled = (order.size() - rank) * hypotheses.get(index).estimate().pValue();
            previous = Math.max(previous, Math.min(1, scaled));
            adjusted[index] = previous;
        }

        final var result = new ArrayList<AdjustedHypothesis>(hypotheses.size());
        for (int index = 0; index < hypotheses.size(); index++) {
            final var hypothesis = hypotheses.get(index);
            result.add(new AdjustedHypothesis(
                hypothesis.code(),
                hypothesis.estimate(),
                adjusted[index],
                hypothesis.estimate().lower() > 0 && adjusted[index] <= FAMILY_ALPHA
            ));
        }
        return List.copyOf(result);
    }

    private static void validatePair(double[] first, double[] second) {
        validateObservations(first);
        validateObservations(second);
        if (first.length != second.length) {
            throw new IllegalArgumentException("Paired observations must have the same length");
        }
    }

    private static void validateEstimate(Estimate estimate) {
        Objects.requireNonNull(estimate, "estimate");
        finite(estimate.point(), "estimate point");
        finite(estimate.lower(), "estimate lower bound");
        finite(estimate.upper(), "estimate upper bound");
        finite(estimate.standardError(), "estimate standard error");
        finite(estimate.pValue(), "estimate p-value");
        finite(estimate.sumSquaredDeviations(), "estimate sum of squared deviations");
        if (estimate.lower() > estimate.point() || estimate.point() > estimate.upper()) {
            throw new IllegalArgumentException("Estimate point must lie inside its confidence interval");
        }
        if (estimate.standardError() < 0) {
            throw new IllegalArgumentException("Estimate standard error must not be negative");
        }
        if (estimate.pValue() < 0 || estimate.pValue() > 1) {
            throw new IllegalArgumentException("Estimate p-value must lie inside [0, 1]");
        }
        if (estimate.sampleSize() < 2 || estimate.sumSquaredDeviations() < 0) {
            throw new IllegalArgumentException("Estimate sample metadata is invalid");
        }
    }

    private static void validateObservations(double[] observations) {
        Objects.requireNonNull(observations, "observations");
        if (observations.length < 2) {
            throw new IllegalArgumentException("At least two observations are required");
        }
        for (final double observation : observations) {
            finite(observation, "observation");
        }
    }

    private static double finite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return value;
    }

    record Estimate(
        double point,
        double lower,
        double upper,
        double standardError,
        double pValue,
        int sampleSize,
        double sumSquaredDeviations
    ) {
    }

    record Hypothesis(String code, Estimate estimate) {
        Hypothesis {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(estimate, "estimate");
        }
    }

    record AdjustedHypothesis(
        String code,
        Estimate estimate,
        double adjustedPValue,
        boolean accepted
    ) {
    }
}
