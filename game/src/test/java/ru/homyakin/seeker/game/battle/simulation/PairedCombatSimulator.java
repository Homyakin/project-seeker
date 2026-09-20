package ru.homyakin.seeker.game.battle.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Supplier;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.ConfidenceInterval;

/** Runs the same deterministic sample with both team orders and reports side-neutral wins. */
public final class PairedCombatSimulator {
    static final int BOOTSTRAP_RESAMPLES = 20_000;
    static final int BOOTSTRAP_LOWER_RANK = 500;
    static final int BOOTSTRAP_UPPER_RANK = 19_500;

    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final String BOOTSTRAP_STREAM = "paired-bootstrap-v1/";

    private final CombatSimulator simulator = new CombatSimulator();

    public PairedCombatReport run(PairedCombatRequest request) {
        final var evaluatedFirst = simulator.run(simulationRequest(request, false));
        final var opponentsFirst = simulator.run(simulationRequest(request, true));
        final var pairOutcomes = pairOutcomes(
            evaluatedFirst.iterationWins(),
            opponentsFirst.iterationWins()
        );
        final int totalBattles = Math.multiplyExact(request.iterations(), 2);
        final int evaluatedWins = pairOutcomes.stream().mapToInt(PairOutcome::evaluatedWins).sum();
        final int firstSideWins = Math.addExact(evaluatedFirst.wins(), opponentsFirst.wins());
        final long resamplingSeed = bootstrapSeed(request.seed(), request.scenarioCode());
        return new PairedCombatReport(
            request,
            evaluatedFirst,
            opponentsFirst,
            pairOutcomes,
            evaluatedWins,
            totalBattles,
            (double) evaluatedWins / totalBattles,
            pairedBootstrap95(pairOutcomes, resamplingSeed),
            (double) firstSideWins / totalBattles,
            resamplingSeed
        );
    }

    /**
     * The first list records whether the evaluated team won while going first. The second records whether the
     * opposing team won while going first, so it is inverted before both outcomes at the same index are joined.
     */
    static List<PairOutcome> pairOutcomes(
        List<Boolean> evaluatedFirstWins,
        List<Boolean> opponentsFirstWins
    ) {
        if (evaluatedFirstWins.isEmpty() || evaluatedFirstWins.size() != opponentsFirstWins.size()) {
            throw new IllegalArgumentException("Both orders must contain the same positive number of outcomes");
        }
        final var result = new ArrayList<PairOutcome>(evaluatedFirstWins.size());
        for (int index = 0; index < evaluatedFirstWins.size(); index++) {
            result.add(new PairOutcome(
                index,
                evaluatedFirstWins.get(index),
                !opponentsFirstWins.get(index)
            ));
        }
        return List.copyOf(result);
    }

    /**
     * Resamples complete seed pairs. Each pair contributes 0, 1/2 or 1, preserving the dependency between the two
     * team orders. The exact preregistered nearest ranks are 500 and 19,500 among 20,000 sorted estimates.
     */
    static ConfidenceInterval pairedBootstrap95(List<PairOutcome> pairs, long seed) {
        if (pairs.isEmpty()) {
            throw new IllegalArgumentException("At least one seed pair is required");
        }
        return bootstrapPercentile95(pairedBootstrapEstimates(pairs, seed));
    }

    /**
     * Sampling empirical scores is exactly a three-category multinomial experiment. The first binomial draw chooses
     * all non-zero scores, and the second chooses full wins among them. This is distribution-equivalent to drawing
     * {@code pairs.size()} individual indices, but its cost does not grow with that number for every resample.
     */
    static double[] pairedBootstrapEstimates(List<PairOutcome> pairs, long seed) {
        if (pairs.isEmpty()) {
            throw new IllegalArgumentException("At least one seed pair is required");
        }
        final var categoryCounts = new int[3];
        for (final var pair : pairs) {
            categoryCounts[pair.evaluatedWins()]++;
        }
        final int sampleSize = pairs.size();
        final int observedNonZero = categoryCounts[1] + categoryCounts[2];
        final double nonZeroProbability = (double) observedNonZero / sampleSize;
        final double fullWinGivenNonZero = observedNonZero == 0
            ? 0
            : (double) categoryCounts[2] / observedNonZero;
        final var random = new SplittableRandom(seed);
        final var estimates = new double[BOOTSTRAP_RESAMPLES];
        final var nonZeroCdf = binomialCdf(sampleSize, nonZeroProbability);
        final Map<Integer, double[]> fullWinCdfs = new HashMap<>();
        for (int resample = 0; resample < BOOTSTRAP_RESAMPLES; resample++) {
            final int nonZero = sampleBinomial(sampleSize, nonZeroProbability, nonZeroCdf, random);
            final var fullWinCdf = fullWinCdfs.computeIfAbsent(
                nonZero,
                trials -> binomialCdf(trials, fullWinGivenNonZero)
            );
            final int fullWins = sampleBinomial(nonZero, fullWinGivenNonZero, fullWinCdf, random);
            estimates[resample] = (nonZero + fullWins) / (2.0 * sampleSize);
        }
        return estimates;
    }

    static ConfidenceInterval bootstrapPercentile95(double[] estimates) {
        if (estimates.length != BOOTSTRAP_RESAMPLES) {
            throw new IllegalArgumentException("Expected exactly " + BOOTSTRAP_RESAMPLES + " bootstrap estimates");
        }
        final var sorted = estimates.clone();
        Arrays.sort(sorted);
        return new ConfidenceInterval(
            sorted[BOOTSTRAP_LOWER_RANK - 1],
            sorted[BOOTSTRAP_UPPER_RANK - 1]
        );
    }

    private static double[] binomialCdf(int trials, double probability) {
        if (trials < 0 || probability < 0 || probability > 1 || !Double.isFinite(probability)) {
            throw new IllegalArgumentException("Invalid binomial parameters");
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
        double cumulative = 0;
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
        int low = 0;
        int high = cdf.length - 1;
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

    static long bootstrapSeed(long battleSeed, String scenarioCode) {
        var hash = FNV_OFFSET_BASIS;
        final var streamName = BOOTSTRAP_STREAM + Objects.requireNonNull(scenarioCode);
        for (int index = 0; index < streamName.length(); index++) {
            hash ^= streamName.charAt(index);
            hash *= FNV_PRIME;
        }
        var mixed = battleSeed ^ hash;
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    private static CombatSimulationRequest simulationRequest(PairedCombatRequest request, boolean reverse) {
        final var composition = reverse
            ? request.opponentsLabel() + " → " + request.evaluatedLabel()
            : request.evaluatedLabel() + " → " + request.opponentsLabel();
        return new CombatSimulationRequest(
            request.family(),
            request.scenarioCode(),
            composition,
            1,
            request.partySize(),
            request.iterations(),
            request.maxRounds(),
            request.seed(),
            reverse ? reversed(request.teamsFactory()) : request.teamsFactory()
        );
    }

    private static Supplier<CombatSimulationTeams> reversed(Supplier<CombatSimulationTeams> factory) {
        return () -> {
            final var teams = factory.get();
            return new CombatSimulationTeams(teams.opponents(), teams.evaluatedTeam());
        };
    }

    public record PairedCombatRequest(
        String family,
        String scenarioCode,
        String evaluatedLabel,
        String opponentsLabel,
        int partySize,
        int iterations,
        int maxRounds,
        long seed,
        Supplier<CombatSimulationTeams> teamsFactory
    ) {
        public PairedCombatRequest {
            family = requireText(family, "family");
            scenarioCode = requireText(scenarioCode, "scenarioCode");
            evaluatedLabel = requireText(evaluatedLabel, "evaluatedLabel");
            opponentsLabel = requireText(opponentsLabel, "opponentsLabel");
            if (partySize <= 0) {
                throw new IllegalArgumentException("partySize must be positive");
            }
            if (iterations <= 0 || iterations > Integer.MAX_VALUE / 2) {
                throw new IllegalArgumentException("iterations must be in 1.." + Integer.MAX_VALUE / 2);
            }
            if (maxRounds <= 0) {
                throw new IllegalArgumentException("maxRounds must be positive");
            }
            Objects.requireNonNull(teamsFactory, "teamsFactory");
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }

    public record PairedCombatReport(
        PairedCombatRequest request,
        CombatSimulationReport evaluatedFirst,
        CombatSimulationReport opponentsFirst,
        List<PairOutcome> pairOutcomes,
        int evaluatedWins,
        int battles,
        double evaluatedWinRate,
        ConfidenceInterval evaluatedWinRate95,
        double firstSideWinRate,
        long bootstrapSeed
    ) {
        public PairedCombatReport {
            pairOutcomes = List.copyOf(pairOutcomes);
            if (!pairOutcomes.isEmpty() && pairOutcomes.size() != request.iterations()) {
                throw new IllegalArgumentException(
                    "Pair outcomes must be empty or contain one value per requested iteration"
                );
            }
        }

        public double firstSideBias() {
            return firstSideWinRate - 0.5;
        }

        /** Keeps aggregate statistics and detailed summaries while releasing all per-iteration outcomes. */
        public PairedCombatReport withoutRawOutcomes() {
            if (pairOutcomes.isEmpty()
                && evaluatedFirst.iterationWins().isEmpty()
                && opponentsFirst.iterationWins().isEmpty()) {
                return this;
            }
            return new PairedCombatReport(
                request,
                evaluatedFirst.withoutRawOutcomes(),
                opponentsFirst.withoutRawOutcomes(),
                List.of(),
                evaluatedWins,
                battles,
                evaluatedWinRate,
                evaluatedWinRate95,
                firstSideWinRate,
                bootstrapSeed
            );
        }
    }

    /** One independent observation formed by matching the same iteration index in both team orders. */
    public record PairOutcome(int index, boolean evaluatedWonWhenFirst, boolean evaluatedWonWhenSecond) {
        public PairOutcome {
            if (index < 0) {
                throw new IllegalArgumentException("index must not be negative");
            }
        }

        public int evaluatedWins() {
            return (evaluatedWonWhenFirst ? 1 : 0) + (evaluatedWonWhenSecond ? 1 : 0);
        }

        public double score() {
            return evaluatedWins() / 2.0;
        }
    }
}
