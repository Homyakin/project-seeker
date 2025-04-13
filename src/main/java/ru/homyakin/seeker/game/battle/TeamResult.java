package ru.homyakin.seeker.game.battle;

import java.util.HashMap;
import java.util.List;

public record TeamResult(
    List<GroupBattleResult> groupResults,
    List<PersonageBattleResult> personageResults
) {
    public static TeamResult of(List<BattlePersonage> personages) {
        final var groupStats = new HashMap<String, BattleStats>();
        final var personageResults = personages.stream()
            .map(it -> {
                if (it.personage() != null && it.personage().tag().isPresent()) {
                    final var stats = groupStats.computeIfAbsent(
                        it.personage().tag().get(),
                        _ -> new BattleStats()
                    );
                    stats.add(it.stats(), it.health());
                }
                return it.toResult();
            })
            .toList();
        final var groupResults = groupStats.entrySet().stream()
            .map(it -> new GroupBattleResult(it.getKey(), GroupBattleStats.of(it.getValue())))
            .toList();
        return new TeamResult(groupResults, personageResults);
    }
}
