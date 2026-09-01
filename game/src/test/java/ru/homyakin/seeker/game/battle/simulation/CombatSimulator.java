package ru.homyakin.seeker.game.battle.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.ConfidenceInterval;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.TeamMetrics;
import ru.homyakin.seeker.utils.RandomUtils;

public final class CombatSimulator {
    private static final double Z_95 = 1.959963984540054;
    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;

    public CombatSimulationReport run(CombatSimulationRequest request) {
        var wins = 0;
        final var rounds = new ArrayList<Integer>(request.iterations());
        final var evaluatedTotals = new MutableTeamTotals();
        final var opponentTotals = new MutableTeamTotals();

        for (int iteration = 0; iteration < request.iterations(); iteration++) {
            final long iterationSeed = iterationSeed(request.seed(), iteration);
            final var sample = RandomUtils.withSeed(iterationSeed, () -> runBattle(request));
            if (sample.evaluatedTeamWin()) {
                wins++;
            }
            rounds.add(sample.rounds());
            evaluatedTotals.add(sample.evaluatedTeam());
            opponentTotals.add(sample.opponents());
        }

        final double winRate = (double) wins / request.iterations();
        return new CombatSimulationReport(
            request.raidType(),
            request.loadout(),
            request.composition(),
            request.difficulty(),
            request.partySize(),
            request.seed(),
            request.iterations(),
            request.maxRounds(),
            wins,
            winRate,
            wilson95(wins, request.iterations()),
            median(rounds),
            percentileNearestRank(rounds, 0.95),
            evaluatedTotals.average(request.iterations()),
            opponentTotals.average(request.iterations())
        );
    }

    private static BattleSample runBattle(CombatSimulationRequest request) {
        final var teams = request.teamsFactory().get();
        if (teams.evaluatedTeam().size() != request.partySize()) {
            throw new IllegalArgumentException(
                "Expected evaluated party size %d, got %d"
                    .formatted(request.partySize(), teams.evaluatedTeam().size())
            );
        }
        final var result = new Battle().process(
            teams.evaluatedTeam(),
            teams.opponents(),
            request.maxRounds()
        );
        final var evaluated = snapshot(teams.evaluatedTeam());
        final var opponents = snapshot(teams.opponents());
        return new BattleSample(
            result.firstWin(),
            result.rounds(),
            evaluated.withDamageDealt(opponents.damageTaken()),
            opponents.withDamageDealt(evaluated.damageTaken())
        );
    }

    private static TeamSample snapshot(List<BattlePersonage> team) {
        final long maxHealth = team.stream().mapToLong(BattlePersonage::maxHealth).sum();
        final long remainingHealth = team.stream().mapToLong(BattlePersonage::health).sum();
        final double remainingHealthPercent = maxHealth == 0
            ? 0
            : remainingHealth * 100.0 / maxHealth;
        return new TeamSample(
            (int) team.stream().filter(BattlePersonage::isAlive).count(),
            remainingHealth,
            remainingHealthPercent,
            0,
            team.stream().mapToLong(BattlePersonage::actualDamageTaken).sum(),
            team.stream().mapToLong(it -> it.battlePersonageStats().turnsCount()).sum()
        );
    }

    static ConfidenceInterval wilson95(int wins, int iterations) {
        if (iterations <= 0 || wins < 0 || wins > iterations) {
            throw new IllegalArgumentException("Expected 0 <= wins <= iterations and iterations > 0");
        }
        final double proportion = (double) wins / iterations;
        final double zSquared = Z_95 * Z_95;
        final double denominator = 1 + zSquared / iterations;
        final double center = (proportion + zSquared / (2 * iterations)) / denominator;
        final double margin = Z_95 * Math.sqrt(
            proportion * (1 - proportion) / iterations + zSquared / (4 * iterations * (double) iterations)
        ) / denominator;
        return new ConfidenceInterval(Math.max(0, center - margin), Math.min(1, center + margin));
    }

    static double median(List<Integer> values) {
        final var sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        if (sorted.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        final int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    static int percentileNearestRank(List<Integer> values, double percentile) {
        if (values.isEmpty() || percentile <= 0 || percentile > 1) {
            throw new IllegalArgumentException("values must not be empty and percentile must be in (0, 1]");
        }
        final var sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        final int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index);
    }

    static long iterationSeed(long seed, int iteration) {
        var mixed = seed + GOLDEN_GAMMA * (iteration + 1L);
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    private record BattleSample(
        boolean evaluatedTeamWin,
        int rounds,
        TeamSample evaluatedTeam,
        TeamSample opponents
    ) {
    }

    private record TeamSample(
        int survivors,
        long remainingHealth,
        double remainingHealthPercent,
        long damageDealt,
        long damageTaken,
        long turns
    ) {
        private TeamSample withDamageDealt(long value) {
            return new TeamSample(
                survivors,
                remainingHealth,
                remainingHealthPercent,
                value,
                damageTaken,
                turns
            );
        }
    }

    private static final class MutableTeamTotals {
        private long survivors;
        private long remainingHealth;
        private double remainingHealthPercent;
        private long damageDealt;
        private long damageTaken;
        private long turns;

        private void add(TeamSample sample) {
            survivors += sample.survivors();
            remainingHealth += sample.remainingHealth();
            remainingHealthPercent += sample.remainingHealthPercent();
            damageDealt += sample.damageDealt();
            damageTaken += sample.damageTaken();
            turns += sample.turns();
        }

        private TeamMetrics average(int iterations) {
            return new TeamMetrics(
                (double) survivors / iterations,
                (double) remainingHealth / iterations,
                remainingHealthPercent / iterations,
                (double) damageDealt / iterations,
                (double) damageTaken / iterations,
                (double) turns / iterations
            );
        }
    }
}
