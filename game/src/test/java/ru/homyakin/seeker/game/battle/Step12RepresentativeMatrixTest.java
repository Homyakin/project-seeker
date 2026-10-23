package ru.homyakin.seeker.game.battle;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.Step12RepresentativeMatrix.Scenario;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class Step12RepresentativeMatrixTest {
    private static final Map<String, ItemObject> OBJECTS = EquipmentCatalogLoader
        .loadValidated(EquipmentCatalogVersion.SCALING_V3).itemObjects().stream()
        .collect(Collectors.toUnmodifiableMap(ItemObject::code, Function.identity()));
    private static final List<Scenario> SCENARIOS = Step12RepresentativeMatrix.scenarios();
    private static final Set<PersonageSlot> HAND_SLOTS = Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND);

    @Test
    void allFiftyFourHandBlocksAreLegalAndPresentInFourOpponentBattles() {
        final var expected = new HashSet<List<String>>();
        final var main = OBJECTS.values().stream()
            .filter(object -> object.slots().equals(Set.of(PersonageSlot.MAIN_HAND))).toList();
        final var off = OBJECTS.values().stream()
            .filter(object -> object.slots().equals(Set.of(PersonageSlot.OFF_HAND))).toList();
        for (final var first : main) {
            for (final var second : off) {
                expected.add(List.of(first.code(), second.code()));
            }
        }
        OBJECTS.values().stream().filter(object -> object.slots().equals(HAND_SLOTS))
            .forEach(object -> expected.add(List.of(object.code())));

        final var actual = Step12RepresentativeBuilds.handBlocks();
        Assertions.assertEquals(54, actual.size());
        Assertions.assertEquals(expected, new HashSet<>(actual));
        final var atTen = SCENARIOS.stream().filter(scenario -> scenario.family().equals("HANDS"))
            .filter(scenario -> scenario.level() == 10).collect(Collectors.groupingBy(
                scenario -> handCodes(scenario.left().getFirst())
            ));
        Assertions.assertEquals(expected, atTen.keySet());
        for (final var scenarios : atTen.values()) {
            Assertions.assertEquals(4, scenarios.size());
            Assertions.assertEquals(
                Set.of("SLASH_PLATE", "BLUNT_LEATHER", "PIERCE_CLOTH", "MAGICAL_ARCANE"),
                scenarios.stream().map(scenario -> scenario.right().getFirst().code()).collect(Collectors.toSet())
            );
            scenarios.forEach(scenario -> assertLegal(scenario.left().getFirst(), scenario.level()));
        }
        Assertions.assertEquals(54, SCENARIOS.stream().filter(scenario -> scenario.family().equals("HANDS_TEAM")).count());
    }

    @Test
    void twentyArmorPairsChangeOnlyTheirOneSlotInEachRecordedEnvironment() {
        final var pairs = Step12RepresentativeBuilds.armorPairs();
        Assertions.assertEquals(20, pairs.size());
        Assertions.assertEquals(20, pairs.stream().map(pair -> pair.oldCode()).distinct().count());
        Assertions.assertEquals(20, pairs.stream().map(pair -> pair.newCode()).distinct().count());
        for (final var pair : pairs) {
            final var oldObject = OBJECTS.get(pair.oldCode());
            final var newObject = OBJECTS.get(pair.newCode());
            Assertions.assertEquals(oldObject.slots(), newObject.slots(), pair.oldCode());
            Assertions.assertEquals(1, oldObject.slots().size(), pair.oldCode());
            Assertions.assertEquals(
                oldObject.defense().orElseThrow().defenseType(),
                newObject.defense().orElseThrow().defenseType(),
                pair.oldCode()
            );
            final var scenarios = SCENARIOS.stream().filter(scenario -> scenario.family().equals("ARMOR_PAIR"))
                .filter(scenario -> scenario.seedGroup().startsWith("ARMOR_" + pair.oldCode() + "_"))
                .collect(Collectors.groupingBy(scenario -> scenario.seedGroup() + "_L" + scenario.level()));
            Assertions.assertEquals(20, scenarios.size(), pair.oldCode());
            for (final var linked : scenarios.values()) {
                Assertions.assertEquals(2, linked.size());
                final var oldCase = linked.stream().filter(scenario -> scenario.code().contains("_OLD_L"))
                    .findFirst().orElseThrow();
                final var newCase = linked.stream().filter(scenario -> scenario.code().contains("_NEW_L"))
                    .findFirst().orElseThrow();
                final var oldBuild = oldCase.left().getFirst();
                final var newBuild = newCase.left().getFirst();
                Assertions.assertTrue(oldBuild.itemCodes().contains(pair.oldCode()));
                Assertions.assertTrue(newBuild.itemCodes().contains(pair.newCode()));
                Assertions.assertEquals(
                    oldBuild.itemCodes().stream().map(code -> code.equals(pair.oldCode()) ? pair.newCode() : code).toList(),
                    newBuild.itemCodes()
                );
                Assertions.assertEquals(oldBuild.position(), newBuild.position());
                Assertions.assertEquals(oldBuild.tactic(), newBuild.tactic());
                Assertions.assertEquals(oldBuild.assignments(), newBuild.assignments());
                Assertions.assertEquals(oldCase.right(), newCase.right());
            }
        }
    }

    @Test
    void everyFullArmorFamilyHasSoloAndTeamBattlesAtTheDevelopmentEndpoints() {
        for (final var family : List.of(
            "CLOTH", "LEATHER", "PLATE", "ARCANE", "NEW_CLOTH", "NEW_LEATHER", "NEW_PLATE", "NEW_ARCANE"
        )) {
            final var solo = SCENARIOS.stream().filter(scenario -> scenario.family().equals("ARMOR_SET"))
                .filter(scenario -> scenario.left().getFirst().code().equals(family)).toList();
            final var teams = SCENARIOS.stream().filter(scenario -> scenario.family().equals("ARMOR_SET_TEAM"))
                .filter(scenario -> scenario.left().get(1).code().equals(family)).toList();
            Assertions.assertEquals(12, solo.size(), family);
            Assertions.assertEquals(3, teams.size(), family);
            Assertions.assertEquals(Set.of(0, 10, 20), solo.stream().map(Scenario::level).collect(Collectors.toSet()));
            Assertions.assertEquals(Set.of(0, 10, 20), teams.stream().map(Scenario::level).collect(Collectors.toSet()));
            for (final var scenario : solo) {
                final var armor = scenario.left().getFirst().itemCodes().stream()
                    .filter(code -> OBJECTS.get(code).slots().stream().noneMatch(HAND_SLOTS::contains)).toList();
                Assertions.assertEquals(Step12RepresentativeBuilds.armor(family), armor);
            }
        }
    }

    @Test
    void matrixUsesEveryObtainableItemAndHasUniqueSortedCasesAtAllFiveControlLevels() {
        Assertions.assertEquals(63, OBJECTS.size());
        final var covered = SCENARIOS.stream().flatMap(Step12RepresentativeMatrixTest::builds)
            .flatMap(build -> build.itemCodes().stream()).collect(Collectors.toSet());
        Assertions.assertEquals(OBJECTS.keySet(), covered);
        Assertions.assertEquals(
            Set.of(0, 3, 6, 10, 20),
            SCENARIOS.stream().map(Scenario::level).collect(Collectors.toSet())
        );
        final var codes = SCENARIOS.stream().map(Scenario::code).toList();
        Assertions.assertEquals(codes.size(), new HashSet<>(codes).size());
        Assertions.assertEquals(codes.stream().sorted().toList(), codes);
        Assertions.assertTrue(SCENARIOS.stream().allMatch(scenario -> !scenario.seedGroup().isBlank()));
        Assertions.assertTrue(SCENARIOS.stream().anyMatch(scenario -> scenario.left().size() == 1));
        Assertions.assertTrue(SCENARIOS.stream().anyMatch(scenario -> scenario.left().size() == 3));
        Assertions.assertTrue(SCENARIOS.stream().anyMatch(scenario -> scenario.left().size() == 7));
    }

    @Test
    void everyScenarioUsesVersionThreeObjectsCompleteEquipmentAndRealModifierCarriers() {
        for (final var scenario : SCENARIOS) {
            Assertions.assertEquals(scenario.left().size(), scenario.right().size(), scenario.code());
            Assertions.assertTrue(Step12RepresentativeMatrix.LEVELS.contains(scenario.level()), scenario.code());
            builds(scenario).forEach(build -> assertLegal(build, scenario.level()));
        }
    }

    @Test
    void riskTeamsHaveFourteenOrTwentyEightRealPointsPerPersonageAndTheSpecifiedLevels() {
        for (final var rarity : List.of(ItemRarity.RARE, ItemRarity.LEGENDARY)) {
            final var expectedLevels = rarity == ItemRarity.RARE ? Set.of(0, 20) : Set.of(0, 3, 6, 10, 20);
            for (final var pair : Step12RepresentativeRisks.pairs(rarity)) {
                final var prefix = pair.code() + "_" + rarity + "_L";
                final var matching = SCENARIOS.stream().filter(scenario -> scenario.family().equals("RISK"))
                    .filter(scenario -> scenario.code().startsWith(prefix))
                    .filter(scenario -> scenario.disabledLeft().isEmpty()).toList();
                Assertions.assertEquals(expectedLevels.size(), matching.size(), prefix);
                Assertions.assertEquals(expectedLevels, matching.stream().map(Scenario::level).collect(Collectors.toSet()));
                for (final var build : Stream.concat(pair.left().stream(), pair.right().stream()).toList()) {
                    final var items = build.items(0);
                    Assertions.assertEquals(rarity.skillPoints() * 7, items.stream().mapToInt(item -> item.skillPoints()).sum());
                    Assertions.assertTrue(items.stream().allMatch(item -> item.rarity() == rarity));
                    final var skillPoints = new EnumMap<ActiveEnum, Integer>(ActiveEnum.class);
                    items.forEach(item -> skillPoints.merge(item.modifier().orElseThrow().activeEnum(), item.skillPoints(), Integer::sum));
                    Assertions.assertTrue(skillPoints.values().stream().allMatch(points -> points <= 8), build.code());
                    assertLegal(build, 0);
                }
            }
        }
    }

    @Test
    void armorReplacementPreservesTheRealAssignmentAndCommonModifiersAreRejected() {
        final var ranked = Step12RepresentativeRisks.ranked("P", ItemRarity.LEGENDARY);
        final var changed = ranked.withArmor("NEW_ARCANE");
        Assertions.assertEquals(ranked.assignments().get("wizard_robe"), changed.assignments().get("phantom_vestment"));
        Assertions.assertEquals(ranked.assignments().get("circlet"), changed.assignments().get("phantom_crown"));
        Assertions.assertEquals(ranked.assignments().get("rapier"), changed.assignments().get("rapier"));
        Assertions.assertEquals(28, changed.items(0).stream().mapToInt(item -> item.skillPoints()).sum());
        assertLegal(changed, 20);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Assignment(ActiveEnum.BERSERK, ItemRarity.COMMON));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ranked.withAssignments(
            Map.of("circlet", new Assignment(ActiveEnum.ACCUMULATION, ItemRarity.LEGENDARY))
        ));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Step12RepresentativeBuilds.hand(
            "OVERLAP", List.of("bow", "dagger"), "CLOTH", Position.BACK, TargetingTactic.THREAT
        ));
    }

    private static Stream<Build> builds(Scenario scenario) {
        return Stream.concat(scenario.left().stream(), scenario.right().stream());
    }

    private static List<String> handCodes(Build build) {
        return build.itemCodes().stream()
            .filter(code -> OBJECTS.get(code).slots().stream().anyMatch(HAND_SLOTS::contains)).toList();
    }

    private static void assertLegal(Build build, int level) {
        final var slots = new EnumMap<PersonageSlot, Integer>(PersonageSlot.class);
        for (final var item : build.items(level)) {
            Assertions.assertEquals(OBJECTS.get(item.object().code()), item.object(), build.code());
            Assertions.assertEquals(level, item.enhanceLevel(), build.code());
            item.object().slots().forEach(slot -> slots.merge(slot, 1, Integer::sum));
            if (item.modifier().isPresent()) {
                Assertions.assertNotEquals(ItemRarity.COMMON, item.rarity(), build.code());
                for (final var slot : item.object().slots()) {
                    Assertions.assertTrue(
                        ModifierCompatibility.isCompatible(item.object(), item.modifier().orElseThrow(), slot),
                        build.code() + ": " + item.object().code()
                    );
                }
            } else {
                Assertions.assertEquals(ItemRarity.COMMON, item.rarity(), build.code());
            }
        }
        Assertions.assertEquals(EnumSet.allOf(PersonageSlot.class), slots.keySet(), build.code());
        Assertions.assertTrue(slots.values().stream().allMatch(count -> count == 1), build.code());
    }
}
