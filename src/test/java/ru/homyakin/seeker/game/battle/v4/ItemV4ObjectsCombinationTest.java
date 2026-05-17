package ru.homyakin.seeker.game.battle.v4;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads {@code game-data/item_v4_objects.toml}, builds every loadout with one object per slot,
 * runs {@link Battle}, ranks by {@link BattlePersonage#power()}.
 */
class ItemV4ObjectsCombinationTest {
    private static final String TOML_PATH = "game-data/item_v4_objects.toml";
    private static final double BASE_CRIT_MULTIPLIER = 1.2;

    @Test
    void rankEachItemObjectByPower() {
        final var ranked = loadObjects().stream()
            .map(object -> {
                final var personage = personageFrom(List.of(object));
                final var power = personage.power();
                assertTrue(
                    Double.isFinite(power) && power > 0,
                    () -> object.code() + " has invalid power: " + power
                );
                return new RankedObject(object, power);
            })
            .sorted(Comparator.comparingDouble(RankedObject::power))
            .toList();

        assertFalse(ranked.isEmpty());
        printObjectPowerStats("objects", ranked);
        printObjectPowerStatsBySlot(ranked);
    }

    @Test
    void rankAllCombinationsByPower() {
        final var objects = loadObjects();
        final var bySlot = groupBySlot(objects);
        final var combinations = allSlotCombinations(bySlot);
        final var baseline = personageFrom(combinations.getFirst());

        final var ranked = combinations.stream()
            .map(codes -> {
                final var personage = personageFrom(codes);
                assertDoesNotThrow(() -> new Battle().process(
                    List.of(personage),
                    List.of(baseline)
                ));
                return new RankedLoadout(codes, personage.power());
            })
            .sorted(Comparator.comparingDouble(RankedLoadout::power))
            .toList();

        assertFalse(ranked.isEmpty());
        printPowerStats(ranked);
    }

    @Test
    void referenceLoadoutsMatchTargetStats() {
        final var objectsByCode = loadObjects().stream()
            .collect(Collectors.toMap(ItemObject::code, object -> object));

        assertLoadoutStats(
            objectsByCode,
            List.of("mace", "tower_shield", "breastplate", "greaves", "sabatons", "great_helm", "gauntlets"),
            new LoadoutStats(1850, DefenseType.PLATE, 160, AttackType.BLUNT, 300, 1, 2, 1, 1.25, 110, 56)
        );
        assertLoadoutStats(
            objectsByCode,
            List.of("spear", "dagger", "cuirass", "leather_chausses", "boots", "leather_helm", "leather_gloves"),
            new LoadoutStats(1250, DefenseType.LEATHER, 106, AttackType.PIERCE, 370, 2, 10, 12, 1.55, 165, 13)
        );
        assertLoadoutStats(
            objectsByCode,
            List.of("staff", "orb", "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves"),
            new LoadoutStats(1250, DefenseType.CLOTH, 106, AttackType.MAGICAL, 370, 3, 10, 7, 1.55, 150, 10)
        );
    }

    @Test
    void objectStatsStayWithinCaps() {
        for (final var object : loadObjects()) {
            assertTrue(object.critChance() <= 5, () -> object.code() + " critChance is above cap");
            assertTrue(object.dodgeChance() <= 5, () -> object.code() + " dodgeChance is above cap");
            assertTrue(object.critMultiplier() <= 0.1, () -> object.code() + " critMultiplier is above cap");
            assertTrue(object.speed() <= 50, () -> object.code() + " speed is above cap");
            assertTrue(object.baseThreat() <= 20, () -> object.code() + " baseThreat is above cap");
        }
    }

    private static void printObjectPowerStatsBySlot(List<RankedObject> rankedAscending) {
        System.out.println("objects by slot:");
        for (final var slot : PersonageSlot.values()) {
            final var slotRanked = rankedAscending.stream()
                .filter(rankedObject -> rankedObject.object().slots().contains(slot))
                .toList();
            if (!slotRanked.isEmpty()) {
                printObjectPowerStats(slot.name(), slotRanked);
            }
        }
    }

    private static void printObjectPowerStats(String title, List<RankedObject> rankedAscending) {
        final var powers = rankedAscending.stream().mapToDouble(RankedObject::power).toArray();
        final var percentile = new Percentile();
        final var avg = Arrays.stream(powers).average().orElse(0);

        System.out.println(title + ": " + powers.length);
        System.out.printf("min: %.2f%n", powers[0]);
        System.out.printf("max: %.2f%n", powers[powers.length - 1]);
        System.out.printf("avg: %.2f%n", avg);

        final int[] percentiles = {1, 5, 10, 25, 50, 75, 90, 95, 99};
        for (final int p : percentiles) {
            final var value = percentile.evaluate(powers, p);
            System.out.printf("p%d: %.2f  %s%n", p, value, objectAtPower(rankedAscending, value).code());
        }

        System.out.printf("min object: %.2f  %s%n", powers[0], rankedAscending.getFirst().object().code());
        System.out.printf("max object: %.2f  %s%n", powers[powers.length - 1], rankedAscending.getLast().object().code());
    }

    private static ItemObject objectAtPower(List<RankedObject> rankedAscending, double targetPower) {
        return rankedAscending.stream()
            .min(Comparator.comparingDouble(entry -> Math.abs(entry.power() - targetPower)))
            .orElseThrow()
            .object();
    }

    private static void printPowerStats(List<RankedLoadout> rankedAscending) {
        final var powers = rankedAscending.stream().mapToDouble(RankedLoadout::power).toArray();
        final var percentile = new Percentile();
        final var avg = Arrays.stream(powers).average().orElse(0);

        System.out.println("combinations: " + powers.length);
        System.out.printf("min: %.2f%n", powers[0]);
        System.out.printf("max: %.2f%n", powers[powers.length - 1]);
        System.out.printf("avg: %.2f%n", avg);

        final int[] percentiles = {1, 5, 10, 25, 50, 75, 90, 95, 99};
        for (final int p : percentiles) {
            final var value = percentile.evaluate(powers, p);
            System.out.printf("p%d: %.2f  %s%n", p, value, formatCodes(loadoutAtPower(rankedAscending, value)));
        }

        System.out.printf("min loadout: %.2f  %s%n", powers[0], formatCodes(rankedAscending.getFirst().codes()));
        assertTrue(powers[0] > 80_000);
        System.out.printf("max loadout: %.2f  %s%n", powers[powers.length - 1], formatCodes(rankedAscending.getLast().codes()));
        assertTrue(powers[powers.length - 1] < 105_000);
    }

    private static List<ItemObject> loadoutAtPower(List<RankedLoadout> rankedAscending, double targetPower) {
        return rankedAscending.stream()
            .min(Comparator.comparingDouble(loadout -> Math.abs(loadout.power() - targetPower)))
            .orElseThrow()
            .codes();
    }

    private static List<ItemObject> loadObjects() {
        return ResourceUtils.calc(TOML_PATH, ItemV4ObjectsToml::load)
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

    private static List<List<ItemObject>> allSlotCombinations(Map<PersonageSlot, List<ItemObject>> bySlot) {
        final var combinations = new ArrayList<List<ItemObject>>();
        collectSlotCombinations(bySlot, Set.of(), List.of(), combinations);
        for (final var loadout : combinations) {
            final var usedSlots = loadout.stream()
                .flatMap(object -> object.slots().stream())
                .collect(Collectors.toSet());
            assertEquals(Set.copyOf(List.of(PersonageSlot.values())), usedSlots);
            assertEquals(
                usedSlots.size(),
                loadout.stream().mapToInt(object -> object.slots().size()).sum(),
                () -> "Overlapping slots in " + formatCodes(loadout)
            );
        }
        return combinations;
    }

    private static void collectSlotCombinations(
        Map<PersonageSlot, List<ItemObject>> bySlot,
        Set<PersonageSlot> occupiedSlots,
        List<ItemObject> loadout,
        List<List<ItemObject>> combinations
    ) {
        final var nextSlot = Arrays.stream(PersonageSlot.values())
            .filter(slot -> !occupiedSlots.contains(slot))
            .findFirst();
        if (nextSlot.isEmpty()) {
            combinations.add(List.copyOf(loadout));
            return;
        }

        for (final var object : bySlot.get(nextSlot.orElseThrow())) {
            if (!disjoint(object.slots(), occupiedSlots)) {
                continue;
            }
            final var nextOccupiedSlots = new java.util.HashSet<>(occupiedSlots);
            nextOccupiedSlots.addAll(object.slots());

            final var nextLoadout = new ArrayList<>(loadout);
            nextLoadout.add(object);

            collectSlotCombinations(bySlot, Set.copyOf(nextOccupiedSlots), List.copyOf(nextLoadout), combinations);
        }
    }

    private static boolean disjoint(Set<PersonageSlot> slots, Set<PersonageSlot> occupiedSlots) {
        return slots.stream().noneMatch(occupiedSlots::contains);
    }

    private static BattlePersonage personageFrom(List<ItemObject> objects) {
        final var items = objects.stream().map(Item::fromObject).toList();
        return new BattlePersonage(items, Position.FRONT);
    }

    private static String formatCodes(List<ItemObject> objects) {
        return objects.stream()
            .map(ItemObject::code)
            .collect(Collectors.joining(", "));
    }

    private static void assertLoadoutStats(
        Map<String, ItemObject> objectsByCode,
        List<String> codes,
        LoadoutStats expected
    ) {
        final var objects = codes.stream()
            .map(objectsByCode::get)
            .toList();
        assertFalse(objects.contains(null), () -> "Missing object in " + codes);

        final var attack = objects.stream()
            .flatMap(object -> object.attack().stream())
            .filter(itemAttack -> itemAttack.attackType() == expected.attackType())
            .mapToInt(ItemAttack::attack)
            .sum();
        final var maxRange = objects.stream()
            .flatMap(object -> object.attack().stream())
            .mapToInt(ItemAttack::range)
            .max()
            .orElse(1);
        final var defense = objects.stream()
            .flatMap(object -> object.defense().stream())
            .filter(itemDefense -> itemDefense.defenseType() == expected.defenseType())
            .mapToInt(ItemDefense::defense)
            .sum();

        assertEquals(expected.health(), objects.stream().mapToInt(ItemObject::health).sum(), codes.toString());
        assertEquals(expected.defense(), defense, codes.toString());
        assertEquals(expected.attack(), attack, codes.toString());
        assertEquals(expected.range(), maxRange, codes.toString());
        assertEquals(expected.critChance(), objects.stream().mapToInt(ItemObject::critChance).sum(), codes.toString());
        assertEquals(expected.dodgeChance(), objects.stream().mapToInt(ItemObject::dodgeChance).sum(), codes.toString());
        assertEquals(
            expected.critMultiplier(),
            BASE_CRIT_MULTIPLIER + objects.stream().mapToDouble(ItemObject::critMultiplier).sum(),
            0.000_001,
            codes.toString()
        );
        assertEquals(expected.speed(), objects.stream().mapToInt(ItemObject::speed).sum(), codes.toString());
        assertEquals(expected.baseThreat(), objects.stream().mapToInt(ItemObject::baseThreat).sum(), codes.toString());
    }

    private record RankedObject(ItemObject object, double power) {
    }

    private record RankedLoadout(List<ItemObject> codes, double power) {
    }

    private record LoadoutStats(
        int health,
        DefenseType defenseType,
        int defense,
        AttackType attackType,
        int attack,
        int range,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat
    ) {
    }
}
