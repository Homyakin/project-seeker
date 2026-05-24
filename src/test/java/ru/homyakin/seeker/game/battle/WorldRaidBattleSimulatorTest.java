package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleGenerator;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidLaunchedBattleInfo;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingWorldRaid;
import ru.homyakin.seeker.infrastructure.init.saving_models.WorldRaids;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.ResourceUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Single-iteration world raid battle against {@code game-data/prod/world_raids_v4.toml}.
 * Uses random catalog loadouts (same rules as {@link RaidWinrateTest}) and writes JSON battle logs like {@link BattleSimulator}.
 */
class WorldRaidBattleSimulatorTest {
    private static final String WORLD_RAIDS_TOML_PATH = "game-data/prod/world_raids_v4.toml";
    private static final String ITEM_OBJECTS_TOML_PATH = "game-data/item_objects_catalog.toml";
    private static final int GROUP_SIZE = 180;

    private static final ObjectMapper TOML_MAPPER = TomlMapper.builder()
        .addModule(new JavaTimeModule())
        .addModule(new Jdk8Module())
        .build();

    private final WorldRaidBattleGenerator battleGenerator = new WorldRaidBattleGenerator();
    private final Battle battle = new Battle();

    @Test
    void dragonWorldRaidSingleBattle() throws Exception {
        final var raid = loadEnabledRaid();
        final var launchedInfo = battleGenerator.launchedFromTemplate(raid.info());
        final var enemies = battleGenerator.generate(launchedInfo);
        final var objectsBySlot = groupBySlot(loadObjects());
        final var participants = randomTeam(objectsBySlot, GROUP_SIZE);
        final var avgPower = participants.stream().mapToDouble(BattlePersonage::power).average().orElse(0);

        System.out.printf("%n=== World raid '%s' — single battle ===%n", raid.code());
        System.out.printf(
            "Enemies: %d (total HP=%d), participants: %d (avg power=%.2f)%n",
            launchedInfo.enemiesCount(),
            launchedInfo.totalHealth(),
            participants.size(),
            avgPower
        );

        final var result = battle.process(enemies, participants);
        final var remainedInfo = battleGenerator.remainedInfo(launchedInfo, enemies, result.personageStats());
        final var participantsWin = !result.firstWin();

        writeBattleLogs(result, raid.code());
        printBattleSummary(result, participants, launchedInfo, remainedInfo, participantsWin);
    }

    private static SavingWorldRaid loadEnabledRaid() {
        return loadWorldRaids().raid().stream()
            .filter(SavingWorldRaid::isEnabled)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No enabled world raid in " + WORLD_RAIDS_TOML_PATH));
    }

    private static WorldRaids loadWorldRaids() {
        return ResourceUtils.calc(WORLD_RAIDS_TOML_PATH, WorldRaidBattleSimulatorTest::parseWorldRaids)
            .orElseThrow(() -> new IllegalStateException("Missing " + WORLD_RAIDS_TOML_PATH));
    }

    private static WorldRaids parseWorldRaids(InputStream stream) {
        try {
            return TOML_MAPPER.readValue(stream, WorldRaids.class);
        } catch (Exception e) {
            throw new IllegalStateException("Can't parse " + WORLD_RAIDS_TOML_PATH, e);
        }
    }

    private static List<BattlePersonage> randomTeam(Map<PersonageSlot, List<ItemObject>> objectsBySlot, int groupSize) {
        return IntStream.range(0, groupSize)
            .mapToObj(ignored -> personageFrom(randomLoadout(objectsBySlot)))
            .toList();
    }

    private static List<ItemObject> randomLoadout(Map<PersonageSlot, List<ItemObject>> objectsBySlot) {
        return randomLoadout(objectsBySlot, Set.of(), new ArrayList<>());
    }

    private static List<ItemObject> randomLoadout(
        Map<PersonageSlot, List<ItemObject>> objectsBySlot,
        Set<PersonageSlot> occupiedSlots,
        List<ItemObject> loadout
    ) {
        final var nextSlot = Arrays.stream(PersonageSlot.values())
            .filter(slot -> !occupiedSlots.contains(slot))
            .findFirst();
        if (nextSlot.isEmpty()) {
            return List.copyOf(loadout);
        }

        final var candidates = objectsBySlot.get(nextSlot.orElseThrow()).stream()
            .filter(object -> disjoint(object.slots(), occupiedSlots))
            .toList();
        final var object = RandomUtils.getRandomElement(candidates);

        final var nextOccupiedSlots = new HashSet<>(occupiedSlots);
        nextOccupiedSlots.addAll(object.slots());
        loadout.add(object);

        return randomLoadout(objectsBySlot, Set.copyOf(nextOccupiedSlots), loadout);
    }

    private static List<ItemObject> loadObjects() {
        return ResourceUtils.calc(ITEM_OBJECTS_TOML_PATH, ItemObjectsToml::load)
            .orElseThrow(() -> new IllegalStateException("Missing " + ITEM_OBJECTS_TOML_PATH))
            .itemObjects();
    }

    private static Map<PersonageSlot, List<ItemObject>> groupBySlot(List<ItemObject> objects) {
        final var bySlot = new EnumMap<PersonageSlot, List<ItemObject>>(PersonageSlot.class);
        for (final var slot : PersonageSlot.values()) {
            final var slotObjects = objects.stream()
                .filter(object -> object.slots().contains(slot))
                .toList();
            assertFalse(
                slotObjects.isEmpty(),
                () -> "No objects for slot " + slot + " in " + ITEM_OBJECTS_TOML_PATH
            );
            bySlot.put(slot, slotObjects);
        }
        return bySlot;
    }

    private static boolean disjoint(Set<PersonageSlot> slots, Set<PersonageSlot> occupiedSlots) {
        return slots.stream().noneMatch(occupiedSlots::contains);
    }

    private static BattlePersonage personageFrom(List<ItemObject> objects) {
        final var items = objects.stream().map(Item::fromObject).toList();
        return new BattlePersonage(items, positionForMaxRange(maxAttackRange(objects)));
    }

    private static int maxAttackRange(List<ItemObject> objects) {
        return objects.stream()
            .flatMap(object -> object.attack().stream())
            .mapToInt(attack -> attack.range())
            .max()
            .orElse(1);
    }

    private static Position positionForMaxRange(int maxRange) {
        return switch (maxRange) {
            case 1 -> Position.FRONT;
            case 2 -> Position.MID;
            default -> Position.BACK;
        };
    }

    private void writeBattleLogs(BattleResult result, String raidCode) throws Exception {
        final var mapper = JsonMapper.builder()
            .addModule(new Jdk8Module())
            .build();
        final var writer = mapper.writerWithDefaultPrettyPrinter();
        final var outDir = Path.of("target", "world-raid-battle-log", raidCode);
        Files.createDirectories(outDir);

        writer.writeValue(outDir.resolve("battle-init.json").toFile(), result.initState());
        writer.writeValue(outDir.resolve("battle-action-log.json").toFile(), result.actionLog().events());
    }

    private static void printBattleSummary(
        BattleResult result,
        List<BattlePersonage> participants,
        WorldRaidLaunchedBattleInfo initialInfo,
        WorldRaidLaunchedBattleInfo remainedInfo,
        boolean participantsWin
    ) {
        final var stats = result.personageStats();
        int aliveParticipants = 0;
        int initialParticipantsHp = 0;
        int remainParticipantsHp = 0;
        for (final var personage : participants) {
            final var personageStats = stats.get(personage.id());
            initialParticipantsHp += personageStats.initialHealth();
            if (!personageStats.isDead()) {
                aliveParticipants++;
                remainParticipantsHp += personageStats.remainHealth();
            }
        }

        System.out.printf("Participants win: %s%n", participantsWin);
        System.out.printf("Rounds: %d%n", result.rounds());
        System.out.printf(
            "Remaining participants: %d / %d (HP %d / %d)%n",
            aliveParticipants,
            participants.size(),
            remainParticipantsHp,
            initialParticipantsHp
        );
        System.out.println("Remaining participant personages:");
        for (int i = 0; i < participants.size(); i++) {
            final var personage = participants.get(i);
            final var personageStats = stats.get(personage.id());
            if (personageStats.isDead()) {
                continue;
            }
            System.out.printf(
                "  #%d: health=%d/%d, position=%s, range=%d, power=%.2f, damageDealt=%d%n",
                i + 1,
                personageStats.remainHealth(),
                personageStats.initialHealth(),
                personage.startPosition(),
                personage.range(),
                personage.power(),
                personageStats.damageDealt()
            );
        }
        System.out.printf(
            "Remaining enemies: %d / %d (HP %d / %d)%n",
            remainedInfo.enemiesCount(),
            initialInfo.enemiesCount(),
            remainedInfo.totalHealth(),
            initialInfo.totalHealth()
        );
        System.out.println("Remaining world raid personages:");
        for (int i = 0; i < remainedInfo.personagesOrEmpty().size(); i++) {
            final var personage = remainedInfo.personagesOrEmpty().get(i);
            System.out.printf(
                "  #%d: health=%d, position=%s, attacks=%d, defenses=%d%n",
                i + 1,
                personage.health(),
                personage.position(),
                personage.attacksOrEmpty().size(),
                personage.defensesOrEmpty().size()
            );
        }
        System.out.printf("Battle logs written to target/world-raid-battle-log/%n");
    }
}
