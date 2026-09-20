package ru.homyakin.seeker.game.battle;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

/** Statistical procedures preregistered for the step 12 paired causal experiments. */
final class Step12CausalStatistics {
    static final int RESAMPLES = 20_000;
    static final int LOWER_RANK = 500;
    static final int UPPER_RANK = 19_500;
    static final int MAX_FAST_MEAN_CATEGORIES = 32;

    private Step12CausalStatistics() {
    }

    /** Resamples complete seed observations and estimates their arithmetic mean. */
    static Estimate mean(double[] observations, long seed) {
        validateObservations(observations, "observations");
        final var random = new SplittableRandom(seed);
        final var categories = halfUnitMeanCategories(observations);
        final var estimates = categories == null
            ? directMeanEstimates(observations, random)
            : halfUnitMeanEstimates(categories, observations.length, random);
        return estimate(arithmeticMean(observations), estimates);
    }

    /** Resamples complete seed pairs and estimates the mean of {@code first - second}. */
    static Estimate pairedDifference(double[] first, double[] second, long seed) {
        validatePair(first, second);
        final var differences = new double[first.length];
        for (int index = 0; index < first.length; index++) {
            differences[index] = finite(first[index] - second[index], "paired difference");
        }
        return mean(differences, seed);
    }

    /**
     * Resamples complete numerator/denominator pairs and recalculates the ratio of sums in every resample.
     * A zero denominator is an invalid experiment rather than a removable observation.
     */
    static Estimate ratioOfSums(double[] numerators, double[] denominators, long seed) {
        validatePair(numerators, denominators);
        final double observedNumerator = sum(numerators);
        final double observedDenominator = sum(denominators);
        final double observed = ratio(observedNumerator, observedDenominator, "observed ratio");
        final var estimates = new double[RESAMPLES];
        final var random = new SplittableRandom(seed);
        for (int resample = 0; resample < RESAMPLES; resample++) {
            var numerator = 0.0;
            var denominator = 0.0;
            for (int draw = 0; draw < numerators.length; draw++) {
                final int index = random.nextInt(numerators.length);
                numerator += numerators[index];
                denominator += denominators[index];
            }
            estimates[resample] = ratio(numerator, denominator, "resampled ratio");
        }
        return estimate(observed, estimates);
    }

    /**
     * Resamples complete seed observations and recalculates
     * {@code (sum(leftNumerator) / sum(leftDenominator))
     * / (sum(rightNumerator) / sum(rightDenominator))} in every resample.
     */
    static Estimate ratioOfRatios(
        double[] leftNumerators,
        double[] leftDenominators,
        double[] rightNumerators,
        double[] rightDenominators,
        long seed
    ) {
        validateSameLength(
            leftNumerators,
            leftDenominators,
            rightNumerators,
            rightDenominators
        );
        final double observed = ratioOfRatios(
            sum(leftNumerators),
            sum(leftDenominators),
            sum(rightNumerators),
            sum(rightDenominators),
            "observed ratio of ratios"
        );
        final var estimates = new double[RESAMPLES];
        final var random = new SplittableRandom(seed);
        for (int resample = 0; resample < RESAMPLES; resample++) {
            var leftNumerator = 0.0;
            var leftDenominator = 0.0;
            var rightNumerator = 0.0;
            var rightDenominator = 0.0;
            for (int draw = 0; draw < leftNumerators.length; draw++) {
                final int index = random.nextInt(leftNumerators.length);
                leftNumerator += leftNumerators[index];
                leftDenominator += leftDenominators[index];
                rightNumerator += rightNumerators[index];
                rightDenominator += rightDenominators[index];
            }
            estimates[resample] = ratioOfRatios(
                leftNumerator,
                leftDenominator,
                rightNumerator,
                rightDenominator,
                "resampled ratio of ratios"
            );
        }
        return estimate(observed, estimates);
    }

    /** Two-sided paired sign-flip test around a zero mean difference. */
    static double pairedSignFlipPValue(double[] differences, long seed) {
        validateObservations(differences, "differences");
        final double observed = Math.abs(arithmeticMean(differences));
        final var random = new SplittableRandom(seed);
        final var halfUnitDifferences = halfUnitDifferences(differences);
        if (halfUnitDifferences != null) {
            return categoricalSignFlipPValue(halfUnitDifferences, random);
        }
        return directSignFlipPValue(differences, observed, random);
    }

    private static double directSignFlipPValue(
        double[] differences,
        double observed,
        SplittableRandom random
    ) {
        var atLeastAsExtreme = 0;
        for (int resample = 0; resample < RESAMPLES; resample++) {
            var signedSum = 0.0;
            var signBits = 0L;
            var availableBits = 0;
            for (final double difference : differences) {
                if (availableBits == 0) {
                    signBits = random.nextLong();
                    availableBits = Long.SIZE;
                }
                signedSum += (signBits & 1L) == 0L ? -difference : difference;
                signBits >>>= 1;
                availableBits--;
            }
            final double permuted = Math.abs(finite(
                signedSum / differences.length,
                "sign-flip mean"
            ));
            if (permuted >= observed) {
                atLeastAsExtreme++;
            }
        }
        return (1.0 + atLeastAsExtreme) / (RESAMPLES + 1.0);
    }

    private static double categoricalSignFlipPValue(
        HalfUnitDifferences differences,
        SplittableRandom random
    ) {
        var atLeastAsExtreme = 0;
        for (int resample = 0; resample < RESAMPLES; resample++) {
            var permutedHalfUnits = 0L;
            for (final var category : differences.categories()) {
                final int positive = sampleBinomial(
                    category.count(),
                    0.5,
                    category.cdf(),
                    random
                );
                permutedHalfUnits += category.halfUnits() * (2L * positive - category.count());
            }
            if (Math.abs(permutedHalfUnits) >= differences.observedAbsoluteHalfUnits()) {
                atLeastAsExtreme++;
            }
        }
        return (1.0 + atLeastAsExtreme) / (RESAMPLES + 1.0);
    }

    /** Two-sided paired sign-flip test for {@code first - second}. */
    static double pairedSignFlipPValue(double[] first, double[] second, long seed) {
        validatePair(first, second);
        final var differences = new double[first.length];
        for (int index = 0; index < first.length; index++) {
            differences[index] = finite(first[index] - second[index], "paired difference");
        }
        return pairedSignFlipPValue(differences, seed);
    }

    /** Holm step-down adjusted p-values in the same order as the input. */
    static double[] holmAdjustedPValues(double[] pValues) {
        if (pValues == null) {
            throw new NullPointerException("pValues");
        }
        for (final double pValue : pValues) {
            if (!Double.isFinite(pValue) || pValue < 0 || pValue > 1) {
                throw new IllegalArgumentException("p-values must be finite and inside [0, 1]");
            }
        }
        final var order = new Integer[pValues.length];
        for (int index = 0; index < order.length; index++) {
            order[index] = index;
        }
        Arrays.sort(order, Comparator
            .comparingDouble((Integer index) -> pValues[index])
            .thenComparingInt(Integer::intValue));

        final var adjusted = new double[pValues.length];
        var previous = 0.0;
        for (int rank = 0; rank < order.length; rank++) {
            final int index = order[rank];
            final double scaled = (order.length - rank) * pValues[index];
            previous = Math.max(previous, Math.min(1.0, scaled));
            adjusted[index] = previous;
        }
        return adjusted;
    }

    private static double[] directMeanEstimates(
        double[] observations,
        SplittableRandom random
    ) {
        final var estimates = new double[RESAMPLES];
        for (int resample = 0; resample < RESAMPLES; resample++) {
            var sum = 0.0;
            for (int draw = 0; draw < observations.length; draw++) {
                sum += observations[random.nextInt(observations.length)];
            }
            estimates[resample] = finite(sum / observations.length, "resampled mean");
        }
        return estimates;
    }

    /** Sequential conditional binomials are exactly equivalent to sampling the empirical categories. */
    private static double[] halfUnitMeanEstimates(
        List<MeanCategory> categories,
        int sampleSize,
        SplittableRandom random
    ) {
        final int conditionalCount = categories.size() - 1;
        final var probabilities = new double[conditionalCount];
        final var cdfs = new java.util.ArrayList<Map<Integer, double[]>>(conditionalCount);
        var remainingObserved = sampleSize;
        for (int index = 0; index < conditionalCount; index++) {
            final var category = categories.get(index);
            probabilities[index] = (double) category.count() / remainingObserved;
            remainingObserved -= category.count();
            cdfs.add(new HashMap<>());
        }

        final var estimates = new double[RESAMPLES];
        for (int resample = 0; resample < RESAMPLES; resample++) {
            var remainingDraws = sampleSize;
            var sum = 0.0;
            for (int index = 0; index < conditionalCount; index++) {
                final double probability = probabilities[index];
                final var categoryCdfs = cdfs.get(index);
                final var cdf = categoryCdfs.computeIfAbsent(
                    remainingDraws,
                    trials -> binomialCdf(trials, probability)
                );
                final int count = sampleBinomial(remainingDraws, probability, cdf, random);
                sum += categories.get(index).value() * count;
                remainingDraws -= count;
            }
            sum += categories.getLast().value() * remainingDraws;
            estimates[resample] = finite(
                sum / sampleSize,
                "resampled half-unit mean"
            );
        }
        return estimates;
    }

    private static List<MeanCategory> halfUnitMeanCategories(double[] observations) {
        final Map<Double, Integer> counts = new TreeMap<>();
        for (final double observation : observations) {
            if (!isHalfUnit(observation)) {
                return null;
            }
            counts.merge(observation == 0.0 ? 0.0 : observation, 1, Math::addExact);
            if (counts.size() > MAX_FAST_MEAN_CATEGORIES) {
                return null;
            }
        }
        return counts.entrySet().stream()
            .map(entry -> new MeanCategory(entry.getKey(), entry.getValue()))
            .toList();
    }

    private static boolean isHalfUnit(double value) {
        if (!Double.isFinite(value)) {
            return false;
        }
        if (Math.abs(value) >= 0x1.0p51) {
            return true;
        }
        final double scaled = 2 * value;
        return scaled == Math.rint(scaled);
    }

    /** Groups any exactly representable half-unit differences, or selects the generic path. */
    private static HalfUnitDifferences halfUnitDifferences(double[] differences) {
        final Map<Long, Integer> counts = new TreeMap<>();
        var observedHalfUnits = 0L;
        var maximumAbsoluteSum = 0L;
        try {
            for (final double difference : differences) {
                final var halfUnits = halfUnits(difference);
                if (halfUnits == null) {
                    return null;
                }
                final long signedHalfUnits = halfUnits;
                observedHalfUnits = Math.addExact(observedHalfUnits, signedHalfUnits);
                final long magnitude = Math.abs(signedHalfUnits);
                maximumAbsoluteSum = Math.addExact(maximumAbsoluteSum, magnitude);
                if (magnitude != 0) {
                    counts.merge(magnitude, 1, Math::addExact);
                }
            }
        } catch (ArithmeticException exception) {
            return null;
        }
        final List<SignFlipCategory> categories = counts.entrySet().stream()
            .map(entry -> new SignFlipCategory(
                entry.getKey(),
                entry.getValue(),
                binomialCdf(entry.getValue(), 0.5)
            ))
            .toList();
        return new HalfUnitDifferences(categories, Math.abs(observedHalfUnits));
    }

    private static Long halfUnits(double value) {
        final double scaled = 2 * value;
        if (!Double.isFinite(scaled)
            || scaled != Math.rint(scaled)
            || scaled <= -0x1.0p63
            || scaled >= 0x1.0p63) {
            return null;
        }
        return (long) scaled;
    }

    private static double[] binomialCdf(int trials, double probability) {
        if (trials < 0 || probability < 0 || probability > 1 || !Double.isFinite(probability)) {
            throw new IllegalArgumentException("invalid binomial parameters");
        }
        if (probability == 0 || probability == 1 || trials == 0) {
            return new double[0];
        }

        final var weights = new double[trials + 1];
        final int mode = Math.min(trials, (int) Math.floor((trials + 1) * probability));
        weights[mode] = 1;
        final double odds = probability / (1 - probability);
        for (int successes = mode + 1; successes <= trials; successes++) {
            weights[successes] = weights[successes - 1]
                * (trials - successes + 1.0) / successes
                * odds;
        }
        for (int successes = mode - 1; successes >= 0; successes--) {
            weights[successes] = weights[successes + 1]
                * (successes + 1.0) / (trials - successes)
                / odds;
        }

        final double total = Arrays.stream(weights).sum();
        var cumulative = 0.0;
        for (int successes = 0; successes < weights.length; successes++) {
            cumulative += weights[successes] / total;
            weights[successes] = cumulative;
        }
        weights[trials] = 1;
        return weights;
    }

    private static int sampleBinomial(
        int trials,
        double probability,
        double[] cdf,
        SplittableRandom random
    ) {
        if (probability == 0 || trials == 0) {
            return 0;
        }
        if (probability == 1) {
            return trials;
        }
        final double value = random.nextDouble();
        var low = 0;
        var high = cdf.length - 1;
        while (low < high) {
            final int middle = low + (high - low) / 2;
            if (value <= cdf[middle]) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }

    private static Estimate estimate(double observed, double[] estimates) {
        Arrays.sort(estimates);
        return new Estimate(
            observed,
            new ConfidenceInterval(
                estimates[LOWER_RANK - 1],
                estimates[UPPER_RANK - 1]
            )
        );
    }

    private static double arithmeticMean(double[] values) {
        return finite(sum(values) / values.length, "mean");
    }

    private static double sum(double[] values) {
        var result = 0.0;
        for (final double value : values) {
            result += value;
        }
        return finite(result, "sum");
    }

    private static double ratio(double numerator, double denominator, String name) {
        if (denominator == 0.0) {
            throw new IllegalArgumentException(name + " denominator must not be zero");
        }
        return finite(numerator / denominator, name);
    }

    private static double ratioOfRatios(
        double leftNumerator,
        double leftDenominator,
        double rightNumerator,
        double rightDenominator,
        String name
    ) {
        final double left = ratio(leftNumerator, leftDenominator, name + " left ratio");
        final double right = ratio(rightNumerator, rightDenominator, name + " right ratio");
        return ratio(left, right, name);
    }

    private static void validatePair(double[] first, double[] second) {
        validateObservations(first, "first");
        validateObservations(second, "second");
        if (first.length != second.length) {
            throw new IllegalArgumentException("paired arrays must have the same length");
        }
    }

    private static void validateSameLength(double[] first, double[]... others) {
        validateObservations(first, "first");
        for (int index = 0; index < others.length; index++) {
            final var observations = others[index];
            validateObservations(observations, "observations " + (index + 2));
            if (first.length != observations.length) {
                throw new IllegalArgumentException("paired arrays must have the same length");
            }
        }
    }

    private static void validateObservations(double[] observations, String name) {
        if (observations == null) {
            throw new NullPointerException(name);
        }
        if (observations.length == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        for (final double observation : observations) {
            if (!Double.isFinite(observation)) {
                throw new IllegalArgumentException(name + " must contain only finite values");
            }
        }
    }

    private static double finite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " must be finite");
        }
        return value;
    }

    record Estimate(double estimate, ConfidenceInterval confidenceInterval) {
        Estimate {
            finite(estimate, "estimate");
        }
    }

    record ConfidenceInterval(double lower, double upper) {
        ConfidenceInterval {
            finite(lower, "lower confidence limit");
            finite(upper, "upper confidence limit");
            if (lower > upper) {
                throw new IllegalArgumentException("lower confidence limit must not exceed upper limit");
            }
        }

        double width() {
            return upper - lower;
        }
    }

    private record HalfUnitDifferences(
        List<SignFlipCategory> categories,
        long observedAbsoluteHalfUnits
    ) {
        private HalfUnitDifferences {
            categories = List.copyOf(categories);
        }
    }

    private record SignFlipCategory(long halfUnits, int count, double[] cdf) {
    }

    private record MeanCategory(double value, int count) {
    }
}
