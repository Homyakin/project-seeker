package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.ResourceUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Win rate simulation for all raid types via {@link RaidGenerator}.
 * Supports random catalog loadouts ({@link ItemObjectsCombinationTest} slot rules)
 * and {@link DefaultItems} starter gear.
 */
class RaidWinrateTest {
    private static final String TOML_PATH = "game-data/item_objects_catalog.toml";
    private static final List<Item> DEFAULT_ITEMS = List.of(
        DefaultItems.MAIN_FIST,
        DefaultItems.OFF_FIST,
        DefaultItems.SHIRT,
        DefaultItems.PANTS,
        DefaultItems.SHOES
    );
    private static final int ITERATIONS = 1000;
    private static final int MIN_GROUP_SIZE = 1;
    private static final int MAX_GROUP_SIZE = 20;
    private static final int MIN_RAID_LEVEL = 5;
    private static final int MAX_RAID_LEVEL = 10;

    private final RaidGenerator raidGenerator = new RaidGenerator();
    private final Battle battle = new Battle();

    @Test
    void wolfPackWinrateWithRandomCatalogLoadouts() {
        final var objectsBySlot = groupBySlot(loadObjects());
        runWinrateMatrix(
            RaidType.WOLFPACK,
            "wolf pack / random catalog loadouts",
            groupSize -> randomTeam(objectsBySlot, groupSize)
        );
    }

    @Test
    void wolfPackWinrateWithDefaultItems() {
        final var singlePersonage = defaultPersonage();
        runWinrateMatrix(
            RaidType.WOLFPACK,
            "wolf pack / default items (power=%.2f)".formatted(singlePersonage.power()),
            RaidWinrateTest::defaultTeam
        );
    }

    @Test
    void zombieHordeWinrateWithRandomCatalogLoadouts() {
        final var objectsBySlot = groupBySlot(loadObjects());
        runWinrateMatrix(
            RaidType.ZOMBIE_HORDE,
            "zombie horde / random catalog loadouts",
            groupSize -> randomTeam(objectsBySlot, groupSize)
        );
    }

    @Test
    void zombieHordeWinrateWithDefaultItems() {
        final var singlePersonage = defaultPersonage();
        runWinrateMatrix(
            RaidType.ZOMBIE_HORDE,
            "zombie horde / default items (power=%.2f)".formatted(singlePersonage.power()),
            RaidWinrateTest::defaultTeam
        );
    }

    /**
     * Counter pick vs Wolf Pack: MAGICAL weapons (strong vs LEATHER wolves)
     * + PLATE armor (best resist against wolves' SLASH attacks).
     */
    @Test
    void wolfPackWinrateWithCounterPickTeam() {
        final var objectsBySlot = groupBySlot(loadObjects());
        final var counterSlots = counterPickSlots(objectsBySlot, AttackType.MAGICAL, DefenseType.PLATE);
        runWinrateMatrix(
            RaidType.WOLFPACK,
            "wolf pack / counter pick (PIERCE + PLATE)",
            groupSize -> randomTeam(counterSlots, groupSize)
        );
    }

    /**
     * Counter pick vs Zombie Horde: BLUNT weapons (strong vs PLATE zombies)
     * + LEATHER armor (best average resist against zombies' BLUNT/SLASH mix).
     */
    @Test
    void zombieHordeWinrateWithCounterPickTeam() {
        final var objectsBySlot = groupBySlot(loadObjects());
        final var counterSlots = counterPickSlots(objectsBySlot, AttackType.BLUNT, DefenseType.LEATHER);
        runWinrateMatrix(
            RaidType.ZOMBIE_HORDE,
            "zombie horde / counter pick (BLUNT + PLATE)",
            groupSize -> randomTeam(counterSlots, groupSize)
        );
    }

    @Test
    void maggeseFlockWinrateWithRandomCatalogLoadouts() {
        final var objectsBySlot = groupBySlot(loadObjects());
        runWinrateMatrix(
            RaidType.MAGGEESE_FLOCK,
            "maggeese flock / random catalog loadouts",
            groupSize -> randomTeam(objectsBySlot, groupSize)
        );
    }

    @Test
    void maggeseFlockWinrateWithDefaultItems() {
        final var singlePersonage = defaultPersonage();
        runWinrateMatrix(
            RaidType.MAGGEESE_FLOCK,
            "maggeese flock / default items (power=%.2f)".formatted(singlePersonage.power()),
            RaidWinrateTest::defaultTeam
        );
    }

    /**
     * Counter pick vs Maggeese Flock: SLASH weapons (exploit CLOTH's lowest resist, ×0.75 effective defence)
     * + PLATE armour (best resist against the Beak Chargers' SLASH damage, ×1.25 mitigation).
     */
    @Test
    void maggeseFlockWinrateWithCounterPickTeam() {
        final var objectsBySlot = groupBySlot(loadObjects());
        final var counterSlots = counterPickSlots(objectsBySlot, AttackType.SLASH, DefenseType.PLATE);
        runWinrateMatrix(
            RaidType.MAGGEESE_FLOCK,
            "maggeese flock / counter pick (SLASH + PLATE)",
            groupSize -> randomTeam(counterSlots, groupSize)
        );
    }

    @Test
    void myconidColonyWinrateWithRandomCatalogLoadouts() {
        final var objectsBySlot = groupBySlot(loadObjects());
        runWinrateMatrix(
            RaidType.MYCONID_COLONY,
            "myconid colony / random catalog loadouts",
            groupSize -> randomTeam(objectsBySlot, groupSize)
        );
    }

    @Test
    void myconidColonyWinrateWithDefaultItems() {
        final var singlePersonage = defaultPersonage();
        runWinrateMatrix(
            RaidType.MYCONID_COLONY,
            "myconid colony / default items (power=%.2f)".formatted(singlePersonage.power()),
            RaidWinrateTest::defaultTeam
        );
    }

    /**
     * Counter pick vs Myconid Colony: PIERCE weapons (strong vs ARCANE shields, ×0.75 effective defence)
     * + ARCANE armor (best resist against the colony's MAGICAL attacks, ×1.25 mitigation).
     */
    @Test
    void myconidColonyWinrateWithCounterPickTeam() {
        final var objectsBySlot = groupBySlot(loadObjects());
        final var counterSlots = counterPickSlots(objectsBySlot, AttackType.PIERCE, DefenseType.ARCANE);
        runWinrateMatrix(
            RaidType.MYCONID_COLONY,
            "myconid colony / counter pick (PIERCE + ARCANE)",
            groupSize -> randomTeam(counterSlots, groupSize)
        );
    }

    private void runWinrateMatrix(RaidType raidType, String title, IntFunction<List<BattlePersonage>> teamFactory) {
        System.out.printf("%n=== %s ===%n", title);
        printHeader();

        for (int groupSize = MIN_GROUP_SIZE; groupSize <= MAX_GROUP_SIZE; groupSize++) {
            final var winRates = new ArrayList<Double>();
            for (int raidLevel = MIN_RAID_LEVEL; raidLevel <= MAX_RAID_LEVEL; raidLevel++) {
                winRates.add(winRate(raidType, teamFactory, groupSize, raidLevel));
            }
            printRow(groupSize, winRates);
        }
    }

    private double winRate(RaidType raidType, IntFunction<List<BattlePersonage>> teamFactory, int groupSize, int raidLevel) {
        int wins = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            final var personages = teamFactory.apply(groupSize);
            final var enemies = raidGenerator.generate(
                raidType,
                raidEvent(raidLevel),
                personages
            );
            final var result = battle.process(enemies, personages);
            if (!result.firstWin()) {
                wins++;
            }
        }
        return wins * 100.0 / ITERATIONS;
    }

    private static List<BattlePersonage> defaultTeam(int groupSize) {
        return IntStream.range(0, groupSize)
            .mapToObj(ignored -> defaultPersonage())
            .toList();
    }

    private static BattlePersonage defaultPersonage() {
        return new BattlePersonage(DEFAULT_ITEMS, Position.FRONT);
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

    private static LaunchedRaidEvent raidEvent(int raidLevel) {
        final var now = LocalDateTime.of(2026, 1, 1, 0, 0);
        return new LaunchedRaidEvent(
            1L,
            1,
            now,
            now.plusHours(1),
            EventStatus.LAUNCHED,
            new RaidParams(raidLevel, 0)
        );
    }

    private static List<ItemObject> loadObjects() {
        return ResourceUtils.calc(TOML_PATH, ItemObjectsToml::load)
            .orElseThrow(() -> new IllegalStateException("Missing " + TOML_PATH))
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
                () -> "No objects for slot " + slot + " in " + TOML_PATH
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
        return new BattlePersonage(items, Position.FRONT);
    }

    /**
     * Returns a slot→items map where each slot's list is filtered to items whose
     * attack type (if present) matches {@code preferredAttack} and whose defense
     * type (if present) matches {@code preferredDefense}.
     * Falls back to the full list for any slot where filtering would leave it empty.
     */
    private static Map<PersonageSlot, List<ItemObject>> counterPickSlots(
        Map<PersonageSlot, List<ItemObject>> objectsBySlot,
        AttackType preferredAttack,
        DefenseType preferredDefense
    ) {
        final var result = new EnumMap<PersonageSlot, List<ItemObject>>(PersonageSlot.class);
        for (final var entry : objectsBySlot.entrySet()) {
            final var filtered = entry.getValue().stream()
                .filter(obj -> isCounterPick(obj, preferredAttack, preferredDefense))
                .toList();
            result.put(entry.getKey(), filtered.isEmpty() ? entry.getValue() : filtered);
        }
        return result;
    }

    /**
     * An item is a counter-pick match when every stat it carries is the preferred type:
     * an item with attack must use {@code preferredAttack}; an item with defense must
     * use {@code preferredDefense}; a pure-stat item always qualifies.
     */
    private static boolean isCounterPick(ItemObject obj, AttackType preferredAttack, DefenseType preferredDefense) {
        final var attackOk = obj.attack()
            .map(a -> a.attackType() == preferredAttack)
            .orElse(true);
        final var defenseOk = obj.defense()
            .map(d -> d.defenseType() == preferredDefense)
            .orElse(true);
        return attackOk && defenseOk;
    }

    private static void printHeader() {
        System.out.print("group |");
        for (int raidLevel = MIN_RAID_LEVEL; raidLevel <= MAX_RAID_LEVEL; raidLevel++) {
            System.out.printf(" L%-2d |", raidLevel);
        }
        System.out.println();
    }

    private static void printRow(int groupSize, List<Double> winRates) {
        System.out.printf("%5d |", groupSize);
        for (final var winRate : winRates) {
            System.out.printf(" %4.1f |", winRate);
        }
        System.out.println();
    }
}
