package ru.homyakin.seeker.game.battle.simulation;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.simulation.RaidSimulationFixtures.ReferenceBuild;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidPersonage;

class CombatSimulatorTest {
    @Test
    void sameSeedProducesEqualReportAndStableMarkdown() {
        final var request = request(20_260_901L, 40);
        final var simulator = new CombatSimulator();

        final var first = simulator.run(request);
        final var second = simulator.run(request);

        Assertions.assertEquals(first, second);
        Assertions.assertEquals(first.markdown(), second.markdown());
        Assertions.assertEquals(40, first.iterations());
        Assertions.assertEquals(3, first.partySize());
        Assertions.assertTrue(first.winRate95().lower() <= first.winRate());
        Assertions.assertTrue(first.winRate() <= first.winRate95().upper());
        Assertions.assertEquals(
            first.evaluatedTeam().averageDamageDealt(),
            first.opponents().averageDamageTaken()
        );
        Assertions.assertEquals(
            first.opponents().averageDamageDealt(),
            first.evaluatedTeam().averageDamageTaken()
        );
    }

    @Test
    void statisticsUseWilsonMedianAndNearestRankP95() {
        final var interval = CombatSimulator.wilson95(500, 1_000);

        Assertions.assertEquals(0.4690696004, interval.lower(), 1e-10);
        Assertions.assertEquals(0.5309303996, interval.upper(), 1e-10);
        Assertions.assertEquals(2.5, CombatSimulator.median(List.of(100, 2, 3, 1)));
        Assertions.assertEquals(100, CombatSimulator.percentileNearestRank(List.of(100, 2, 3, 1), 0.95));
        Assertions.assertNotEquals(
            CombatSimulator.iterationSeed(7L, 0),
            CombatSimulator.iterationSeed(7L, 1)
        );
    }

    @Test
    void invalidRequestsAndStatisticsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> request(1L, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> invalidRequest(0, 10_000));
        Assertions.assertThrows(IllegalArgumentException.class, () -> invalidRequest(1, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CombatSimulator.wilson95(-1, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CombatSimulator.median(List.of()));
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RaidSimulationFixtures.parseComposition("PLATE_TANK,ARCANE_MAGE", 3)
        );
    }

    @Test
    void stalemateFailsAtConfiguredRoundCap() {
        final var request = new CombatSimulationRequest(
            "TEST",
            "NONE",
            "STALEMATE",
            1,
            1,
            1,
            3,
            1L,
            () -> new CombatSimulationTeams(
                List.of(stalematePersonage()),
                List.of(stalematePersonage())
            )
        );

        final var exception = Assertions.assertThrows(
            IllegalStateException.class,
            () -> new CombatSimulator().run(request)
        );
        Assertions.assertEquals("Battle did not finish within 3 rounds", exception.getMessage());
    }

    private static CombatSimulationRequest request(long seed, int iterations) {
        final var raidType = RaidType.ZOMBIE_HORDE;
        final var composition = List.of(
            ReferenceBuild.LEATHER_PIERCE,
            ReferenceBuild.ARCANE_MAGE,
            ReferenceBuild.PLATE_TANK
        );
        final var raidGenerator = new RaidGenerator();
        return new CombatSimulationRequest(
            raidType.code(),
            RaidSimulationFixtures.loadoutLabel(composition),
            RaidSimulationFixtures.compositionLabel(composition),
            10,
            composition.size(),
            iterations,
            10_000,
            seed,
            () -> {
                final var players = RaidSimulationFixtures.team(composition);
                final var enemies = raidGenerator.generate(raidType, raidEvent(10), players);
                return new CombatSimulationTeams(players, enemies);
            }
        );
    }

    private static BattlePersonage stalematePersonage() {
        return new BattlePersonage(
            new WorldRaidPersonage(
                List.of(),
                10,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                Position.FRONT
            ),
            Position.FRONT
        );
    }

    private static CombatSimulationRequest invalidRequest(int difficulty, int maxRounds) {
        return new CombatSimulationRequest(
            "TEST",
            "NONE",
            "NONE",
            difficulty,
            1,
            1,
            maxRounds,
            1L,
            () -> null
        );
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
}
