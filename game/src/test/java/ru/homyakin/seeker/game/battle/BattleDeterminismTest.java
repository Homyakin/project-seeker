package ru.homyakin.seeker.game.battle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.simulation.RaidSimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.RaidSimulationFixtures.ReferenceBuild;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.utils.RandomUtils;

class BattleDeterminismTest {
    @Test
    void sameSeedReplaysNpcVariantsAndCompleteBattle() {
        final var first = simulate(4_242_424_242L);
        final var second = simulate(4_242_424_242L);

        Assertions.assertEquals(first, second);
    }

    private static BattleSnapshot simulate(long seed) {
        return RandomUtils.withSeed(seed, () -> {
            final var players = RaidSimulationFixtures.team(List.of(
                ReferenceBuild.LEATHER_PIERCE,
                ReferenceBuild.ARCANE_MAGE,
                ReferenceBuild.PLATE_TANK
            ));
            final var enemies = new RaidGenerator().generate(
                RaidType.ZOMBIE_HORDE,
                raidEvent(10),
                players
            );
            final var result = new Battle().process(players, enemies);
            return new BattleSnapshot(
                result.initState(),
                List.copyOf(result.actionLog().events()),
                result.rounds(),
                result.firstWin(),
                Map.copyOf(result.personageStats())
            );
        });
    }

    private static LaunchedRaidEvent raidEvent(int difficulty) {
        final var start = LocalDateTime.of(2026, 1, 1, 0, 0);
        return new LaunchedRaidEvent(
            1L,
            1,
            start,
            start.plusHours(1),
            EventStatus.LAUNCHED,
            new RaidParams(difficulty, 0)
        );
    }

    private record BattleSnapshot(
        BattleInitState initState,
        List<BattleEvent> events,
        int rounds,
        boolean firstWin,
        Map<UUID, BattlePersonageStats> personageStats
    ) {
    }
}
