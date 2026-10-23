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
        "ru/homyakin/seeker/game/personage/power/PowerDisplayService.java",
        "ru/homyakin/seeker/locale/battle/BattleLocalization.java"
    );
    private static final Set<String> DISPLAY_SERVICE_CONSUMERS = Set.of(
        "ru/homyakin/seeker/game/item/catalog/EquipmentReleaseCoordinator.java",
        "ru/homyakin/seeker/game/personage/PersonageService.java",
        "ru/homyakin/seeker/game/personage/power/PowerDisplayService.java",
        "ru/homyakin/seeker/telegram/command/user/report/AnomalyReportExecutor.java",
        "ru/homyakin/seeker/telegram/command/user/report/RaidReportExecutor.java",
        "ru/homyakin/seeker/telegram/command/user/stats/BattleStatsMessageService.java",
        "ru/homyakin/seeker/telegram/command/user/world_raid/UserWorldRaidReportExecutor.java"
    );

    @Test
    public void onlyTemporaryConsumersCanUseLegacyPower() throws IOException {
        Assertions.assertEquals(
            LEGACY_POWER_CONSUMERS,
            filesContaining(".legacyPower(", "::legacyPower")
        );
    }

    @Test
    public void onlyTheVersionedPresentationScaleCanReadReferenceCalculator() throws IOException {
        final var consumers = filesContaining("ReferencePowerCalculator");
        consumers.remove("ru/homyakin/seeker/game/personage/power/ReferencePowerCalculator.java");
        Assertions.assertEquals(Set.of("ru/homyakin/seeker/game/personage/power/PowerDisplayVersion.java"), consumers);
    }

    @Test
    public void referenceDisplayServiceHasOnlyPresentationAndActivationReadinessConsumers() throws IOException {
        Assertions.assertEquals(DISPLAY_SERVICE_CONSUMERS, filesContaining("PowerDisplayService"));
        Assertions.assertEquals(Set.of(
            "ru/homyakin/seeker/game/item/catalog/EquipmentReleaseCoordinator.java",
            "ru/homyakin/seeker/game/personage/power/PowerDisplayService.java",
            "ru/homyakin/seeker/game/personage/power/PowerDisplaySnapshot.java",
            "ru/homyakin/seeker/game/personage/power/PowerDisplayVersion.java"
        ), filesContaining("PowerDisplayVersion"));
        final var coordinator = Files.readString(sourceRoot().resolve(
            "ru/homyakin/seeker/game/item/catalog/EquipmentReleaseCoordinator.java"
        ));
        Assertions.assertFalse(coordinator.contains("referenceValue("));
        Assertions.assertFalse(coordinator.contains("ReferencePowerCalculator"));
        Assertions.assertFalse(coordinator.contains("powerDisplayService.value("));
        Assertions.assertFalse(coordinator.contains("battleReportValue("));
    }

    @Test
    public void savedPowerIsReadOnlyByPersonalReportPresentation() throws IOException {
        Assertions.assertEquals(Set.of(
            "ru/homyakin/seeker/telegram/command/user/report/AnomalyReportExecutor.java",
            "ru/homyakin/seeker/telegram/command/user/report/RaidReportExecutor.java",
            "ru/homyakin/seeker/telegram/command/user/world_raid/UserWorldRaidReportExecutor.java"
        ), filesContaining(".displayedPower()"));
        final var snapshotConsumers = filesContaining("PowerDisplaySnapshot");
        Assertions.assertEquals(Set.of(
            "ru/homyakin/seeker/game/battle/BattlePersonage.java",
            "ru/homyakin/seeker/game/battle/BattlePersonageStats.java",
            "ru/homyakin/seeker/game/personage/power/PowerDisplaySnapshot.java",
            "ru/homyakin/seeker/game/personage/power/PowerDisplayService.java"
        ), snapshotConsumers);
    }

    @Test
    public void battleSnapshotCannotDriveBattleBehavior() throws IOException {
        final var personage = Files.readString(sourceRoot().resolve(
            "ru/homyakin/seeker/game/battle/BattlePersonage.java"
        ));
        Assertions.assertFalse(personage.contains("displayedPower.get("));
        Assertions.assertFalse(personage.contains("displayedPower.orElse("));
        Assertions.assertFalse(personage.contains("displayedPower.map("));
        Assertions.assertFalse(personage.contains("referenceValue("));
    }

    @Test
    public void finalDisplayNumbersCanOnlyFlowIntoTheReadOnlyRanking() throws IOException {
        Assertions.assertEquals(Set.of("ru/homyakin/seeker/game/top/TopService.java"),
            filesContaining(".displayedPowerByPersonageIds("));
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
