package ru.homyakin.seeker.game.battle.simulation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;

/**
 * Property-gated calibration entry point. Its name deliberately does not match Surefire's default test patterns.
 * Run explicitly with {@code -Dtest=RaidCalibrationSimulator -Draid.calibration.enabled=true}.
 */
@EnabledIfSystemProperty(named = "raid.calibration.enabled", matches = "true")
class RaidCalibrationSimulator {
    private static final String PREFIX = "raid.calibration.";

    @Test
    void runConfiguredCell() throws Exception {
        final var raidType = RaidType.valueOf(
            property("raidType", "MYCONID_COLONY").toUpperCase(Locale.ROOT)
        );
        final int difficulty = intProperty("difficulty", 10);
        final int partySize = intProperty("partySize", 3);
        final int iterations = intProperty("iterations", 1_000);
        final int maxRounds = intProperty("maxRounds", 10_000);
        final long seed = longProperty("seed", 20_260_901L);
        final var composition = RaidSimulationFixtures.parseComposition(
            property("composition", "VIRTUAL_DEFAULT"),
            partySize
        );
        final var raidGenerator = new RaidGenerator();
        final var report = new CombatSimulator().run(new CombatSimulationRequest(
            raidType.code(),
            RaidSimulationFixtures.loadoutLabel(composition),
            RaidSimulationFixtures.compositionLabel(composition),
            difficulty,
            partySize,
            iterations,
            maxRounds,
            seed,
            () -> {
                final var players = RaidSimulationFixtures.team(composition);
                final var enemies = raidGenerator.generate(raidType, raidEvent(difficulty), players);
                return new CombatSimulationTeams(players, enemies);
            }
        ));
        final var markdown = report.markdown();
        final var output = Path.of(property("output", "target/raid-calibration-report.md"));
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output, markdown);
        System.out.println(markdown);
        System.out.println("Report: " + output.toAbsolutePath());
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

    private static String property(String name, String defaultValue) {
        return System.getProperty(PREFIX + name, defaultValue);
    }

    private static int intProperty(String name, int defaultValue) {
        return Integer.parseInt(property(name, Integer.toString(defaultValue)));
    }

    private static long longProperty(String name, long defaultValue) {
        return Long.parseLong(property(name, Long.toString(defaultValue)));
    }
}
