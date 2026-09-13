package ru.homyakin.seeker.game.personage.power;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PowerDependencyGuardTest {
    private static final Set<String> LEGACY_POWER_CONSUMERS = Set.of(
        "ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGenerator.java",
        "ru/homyakin/seeker/game/event/raid/generator/MaggeeseFlockGenerator.java",
        "ru/homyakin/seeker/game/event/raid/generator/MyconidColonyGenerator.java",
        "ru/homyakin/seeker/game/event/raid/generator/WolfPackGenerator.java",
        "ru/homyakin/seeker/game/event/raid/generator/ZombieHordeGenerator.java",
        "ru/homyakin/seeker/game/personage/PersonageService.java",
        "ru/homyakin/seeker/game/top/TopService.java",
        "ru/homyakin/seeker/locale/battle/BattleLocalization.java"
    );

    @Test
    public void onlyTemporaryConsumersCanUseLegacyPower() throws IOException {
        Assertions.assertEquals(
            LEGACY_POWER_CONSUMERS,
            filesContaining(".legacyPower(", "::legacyPower")
        );
    }

    @Test
    public void referencePowerHasNoProductionConsumersBeforeDisplaySwitch() throws IOException {
        final var consumers = filesContaining("ReferencePowerCalculator");
        consumers.remove("ru/homyakin/seeker/game/personage/power/ReferencePowerCalculator.java");
        Assertions.assertEquals(Set.of(), consumers);
    }

    private static Set<String> filesContaining(String... tokens) throws IOException {
        final var sourceRoot = sourceRoot();
        final var result = new TreeSet<String>();
        try (var paths = Files.walk(sourceRoot)) {
            for (final var path : paths.filter(it -> it.toString().endsWith(".java")).toList()) {
                final var content = Files.readString(path, StandardCharsets.UTF_8);
                for (final var token : tokens) {
                    if (content.contains(token)) {
                        result.add(sourceRoot.relativize(path).toString());
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static Path sourceRoot() {
        final var moduleRoot = Path.of("src/main/java");
        if (Files.isDirectory(moduleRoot)) {
            return moduleRoot;
        }
        return Path.of("game/src/main/java");
    }
}
