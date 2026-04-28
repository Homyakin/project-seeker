package ru.homyakin.seeker.game.battle.v4;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Templates for Monte Carlo duel simulations. Always call {@link DuelPersonageTemplate#create()}
 * before each {@link Duel#process}; personages are mutated during a duel.
 */
public final class DuelSimulator {
    private DuelSimulator() {
    }

    public record DuelPairSimulationResult(
        DuelPersonageTemplate tracked,
        DuelPersonageTemplate opponent,
        int duelsPerOrder,
        Map<Integer, Long> roundDistribution,
        double winRateAsFirst,
        double winRateAsSecond
    ) {
        @Override
        public String toString() {
            final long totalDuels = roundDistribution.values().stream().mapToLong(Long::longValue).sum();
            final String dist;
            if (totalDuels == 0) {
                dist = "";
            } else {
                dist = roundDistribution.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> String.format(
                        "%dr: %.2f%%",
                        e.getKey(),
                        100.0 * e.getValue() / totalDuels
                    ))
                    .collect(Collectors.joining(", "));
            }
            return String.format(
                "%s vs %s | duels per order=%d | as first=%.4f | as second=%.4f | rounds [%s] (n=%d)",
                tracked,
                opponent,
                duelsPerOrder,
                winRateAsFirst,
                winRateAsSecond,
                dist,
                totalDuels
            );
        }
    }

    /**
     * Runs {@code duelsPerOrder} duels with {@code tracked} going first, then {@code duelsPerOrder}
     * with {@code tracked} going second. Fresh personages are created for every {@link Duel#process}.
     *
     * @param tracked   personage whose win rates are reported
     * @param opponent  the other fighter
     */
    public static DuelPairSimulationResult simulatePair(
        DuelPersonageTemplate tracked,
        DuelPersonageTemplate opponent,
        int duelsPerOrder
    ) {
        /*
        System.out.printf(
            "DuelSimulator: start pair %s vs %s (%d duels per order)%n",
            tracked,
            opponent,
            duelsPerOrder
        );*/
        final var duel = new Duel();
        final Map<Integer, Long> roundDistribution = new TreeMap<>();
        int winsAsFirst = 0;
        int winsAsSecond = 0;

        for (int i = 0; i < duelsPerOrder; i++) {
            final var first = tracked.create();
            final var second = opponent.create();
            final var result = duel.process(first, second);
            roundDistribution.merge(result.rounds(), 1L, Long::sum);
            if (first.isAlive()) {
                winsAsFirst++;
            }
        }
        /*
        System.out.printf(
            "DuelSimulator: %s vs %s — finished %s as first, running %s as second...%n",
            tracked,
            opponent,
            tracked,
            tracked
        );*/
        for (int i = 0; i < duelsPerOrder; i++) {
            final var first = opponent.create();
            final var second = tracked.create();
            final var result = duel.process(first, second);
            roundDistribution.merge(result.rounds(), 1L, Long::sum);
            if (second.isAlive()) {
                winsAsSecond++;
            }
        }
        //System.out.printf("DuelSimulator: done pair %s vs %s%n", tracked, opponent);

        return new DuelPairSimulationResult(
            tracked,
            opponent,
            duelsPerOrder,
            Map.copyOf(roundDistribution),
            winsAsFirst / (double) duelsPerOrder,
            winsAsSecond / (double) duelsPerOrder
        );
    }
}
