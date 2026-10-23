package ru.homyakin.seeker.game.battle.simulation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;

class Step12ScalingV3FixturesTest {
    private static final Map<String, ItemObject> V3_OBJECTS;
    private static final Map<String, Modifier> V3_MODIFIERS;

    static {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        V3_OBJECTS = catalog.itemObjects().stream().collect(Collectors.toUnmodifiableMap(
            ItemObject::code,
            Function.identity()
        ));
        V3_MODIFIERS = catalog.modifiers().stream().collect(Collectors.toUnmodifiableMap(
            Modifier::code,
            Function.identity()
        ));
    }

    @Test
    void remapsEveryFrozenCompositionWithoutChangingItsItemAssignments() {
        for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
            for (final var build : Step12SimulationFixtures.RoleBuild.values()) {
                assertRemapped(
                    build.name() + " +" + level,
                    Step12SimulationFixtures.items(build, level),
                    Step12ScalingV3Fixtures.items(build, level)
                );
            }
            for (final var build : Step12SimulationFixtures.MixedBuild.values()) {
                assertRemapped(
                    build.name() + " +" + level,
                    Step12SimulationFixtures.items(build, level),
                    Step12ScalingV3Fixtures.items(build, level)
                );
            }
        }
    }

    @Test
    void remappedPersonagesKeepPlacementTacticAndSecondSkillFormulaVersion() {
        for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
            for (final var build : Step12SimulationFixtures.RoleBuild.values()) {
                final var personage = Step12ScalingV3Fixtures.personage(build, level);
                Assertions.assertAll(
                    build.name() + " +" + level,
                    () -> Assertions.assertEquals(build.position(), personage.startPosition()),
                    () -> Assertions.assertEquals(build.targetingTactic(), personage.targetingTactic()),
                    () -> Assertions.assertTrue(personage.skillSnapshots().stream().allMatch(snapshot ->
                        snapshot.formulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V2
                    ))
                );
            }
            for (final var build : Step12SimulationFixtures.MixedBuild.values()) {
                final var personage = Step12ScalingV3Fixtures.personage(build, level);
                Assertions.assertAll(
                    build.name() + " +" + level,
                    () -> Assertions.assertEquals(build.position(), personage.startPosition()),
                    () -> Assertions.assertEquals(build.targetingTactic(), personage.targetingTactic()),
                    () -> Assertions.assertTrue(personage.skillSnapshots().stream().allMatch(snapshot ->
                        snapshot.formulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V2
                    ))
                );
            }
        }
    }

    @Test
    void thirdCatalogBowUsesTheNewPrimaryAttackAtEveryControlLevel() {
        for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
            final var oldBow = item(
                Step12SimulationFixtures.items(Step12SimulationFixtures.MixedBuild.ARTILLERIST, level),
                "bow"
            );
            final var newBow = item(
                Step12ScalingV3Fixtures.items(Step12SimulationFixtures.MixedBuild.ARTILLERIST, level),
                "bow"
            );

            Assertions.assertAll(
                "bow +" + level,
                () -> Assertions.assertEquals(300 + 5 * level, oldBow.itemAttacks().getFirst().attack()),
                () -> Assertions.assertEquals(240 + 4 * level, newBow.itemAttacks().getFirst().attack()),
                () -> Assertions.assertEquals(oldBow.itemAttacks().get(1), newBow.itemAttacks().get(1))
            );
        }
    }

    @Test
    void bowDeltaTouchesOnlyArtilleristAndBlockerAndNoPureRole() {
        for (final var build : Step12SimulationFixtures.RoleBuild.values()) {
            final var historical = Step12SimulationFixtures.items(build, 0);
            final var candidate = Step12ScalingV3Fixtures.items(build, 0);
            Assertions.assertAll(
                build.name(),
                () -> Assertions.assertTrue(historical.stream().noneMatch(item ->
                    item.object().code().equals("bow")
                )),
                () -> Assertions.assertEquals(
                    historical.stream().map(Item::object).toList(),
                    candidate.stream().map(Item::object).toList()
                )
            );
        }

        final var buildsWithBow = java.util.Arrays.stream(Step12SimulationFixtures.MixedBuild.values())
            .filter(build -> Step12SimulationFixtures.items(build, 0).stream().anyMatch(item ->
                item.object().code().equals("bow")
            ))
            .collect(Collectors.toUnmodifiableSet());

        Assertions.assertEquals(
            Set.of(
                Step12SimulationFixtures.MixedBuild.ARTILLERIST,
                Step12SimulationFixtures.MixedBuild.BLOCKER
            ),
            buildsWithBow
        );
    }

    private static void assertRemapped(String name, List<Item> source, List<Item> remapped) {
        Assertions.assertEquals(source.size(), remapped.size(), name);
        for (int index = 0; index < source.size(); index++) {
            final var sourceItem = source.get(index);
            final var remappedItem = remapped.get(index);
            Assertions.assertAll(
                name + " item " + index,
                () -> Assertions.assertEquals(sourceItem.object().code(), remappedItem.object().code()),
                () -> Assertions.assertEquals(sourceItem.rarity(), remappedItem.rarity()),
                () -> Assertions.assertEquals(sourceItem.enhanceLevel(), remappedItem.enhanceLevel()),
                () -> Assertions.assertEquals(
                    sourceItem.modifier().map(Modifier::code),
                    remappedItem.modifier().map(Modifier::code)
                ),
                () -> Assertions.assertEquals(
                    V3_OBJECTS.get(remappedItem.object().code()),
                    remappedItem.object()
                ),
                () -> remappedItem.modifier().ifPresent(modifier -> Assertions.assertEquals(
                    V3_MODIFIERS.get(modifier.code()),
                    modifier
                ))
            );
        }
    }

    private static Item item(List<Item> items, String code) {
        return items.stream().filter(candidate -> candidate.object().code().equals(code)).findFirst().orElseThrow();
    }
}
