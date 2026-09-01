package ru.homyakin.seeker.game.battle.simulation;

import java.util.Locale;

public record CombatSimulationReport(
    String raidType,
    String loadout,
    String composition,
    int difficulty,
    int partySize,
    long seed,
    int iterations,
    int maxRounds,
    int wins,
    double winRate,
    ConfidenceInterval winRate95,
    double medianRounds,
    int p95Rounds,
    TeamMetrics evaluatedTeam,
    TeamMetrics opponents
) {
    public String markdown() {
        return String.format(
            Locale.ROOT,
            """
                | Raid | Loadout | Composition | D | N | Seed | Battles | Round cap | Win rate (95%% CI) | Rounds p50/p95 |
                |---|---|---|---:|---:|---:|---:|---:|---:|---:|
                | %s | %s | %s | %d | %d | %d | %d | %d | %.2f%% [%.2f%%, %.2f%%] | %.1f / %d |

                | Team | Survivors | Remaining HP | Remaining HP %% | Damage dealt | Damage taken | Turns |
                |---|---:|---:|---:|---:|---:|---:|
                | Evaluated | %.2f | %.2f | %.2f%% | %.2f | %.2f | %.2f |
                | Opponents | %.2f | %.2f | %.2f%% | %.2f | %.2f | %.2f |
                """,
            escape(raidType),
            escape(loadout),
            escape(composition),
            difficulty,
            partySize,
            seed,
            iterations,
            maxRounds,
            winRate * 100,
            winRate95.lower() * 100,
            winRate95.upper() * 100,
            medianRounds,
            p95Rounds,
            evaluatedTeam.averageSurvivors(),
            evaluatedTeam.averageRemainingHealth(),
            evaluatedTeam.averageRemainingHealthPercent(),
            evaluatedTeam.averageDamageDealt(),
            evaluatedTeam.averageDamageTaken(),
            evaluatedTeam.averageTurns(),
            opponents.averageSurvivors(),
            opponents.averageRemainingHealth(),
            opponents.averageRemainingHealthPercent(),
            opponents.averageDamageDealt(),
            opponents.averageDamageTaken(),
            opponents.averageTurns()
        );
    }

    private static String escape(String value) {
        return value.replace("|", "\\|");
    }

    public record ConfidenceInterval(double lower, double upper) {
    }

    public record TeamMetrics(
        double averageSurvivors,
        double averageRemainingHealth,
        double averageRemainingHealthPercent,
        double averageDamageDealt,
        double averageDamageTaken,
        double averageTurns
    ) {
    }
}
