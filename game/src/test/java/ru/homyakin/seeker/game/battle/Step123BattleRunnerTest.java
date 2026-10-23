package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;

class Step123BattleRunnerTest {
    @Test
    void thirdCatalogExactlyMatchesTheHistoricalRunnerInBothOrders() {
        final var left = List.of(
            Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN),
            Step12RepresentativeBuilds.mixed(MixedBuild.BLOCKER)
        );
        final var right = List.of(
            Step12RepresentativeBuilds.role(RoleBuild.ASSASSIN),
            Step12RepresentativeBuilds.mixed(MixedBuild.ARTILLERIST)
        );
        final var leftGear = left.stream().map(build -> new Gear(build, EquipmentCatalogVersion.SCALING_V3)).toList();
        final var rightGear = right.stream().map(build -> new Gear(build, EquipmentCatalogVersion.SCALING_V3)).toList();
        for (final long seed : List.of(2_026_102_301L, 2_026_102_302L)) {
            for (final int level : List.of(0, 10, 20)) {
                for (final boolean reverse : List.of(false, true)) {
                    for (final var disabledLeft : List.of(Set.<ActiveEnum>of(), Set.of(ActiveEnum.KNOCKBACK))) {
                        final var expected = Step12RepresentativeBattleRunner.observe(
                            left, right, level, seed, reverse, 10_000, disabledLeft, Set.of()
                        );
                        final var actual = Step123BattleRunner.observe(
                            leftGear, rightGear, level, seed, reverse, 10_000, disabledLeft, Set.of()
                        );
                        Assertions.assertEquals(expected, actual, "level=" + level + ", reverse=" + reverse);
                    }
                }
            }
        }
    }

    @Test
    void fourthCatalogUsesTheRevisedSpeedsWithoutMutatingThirdCatalogEquipment() {
        final var build = Step12RepresentativeBuilds.role(RoleBuild.TACTICIAN_MAGICAL).withArmor("NEW_CLOTH");
        for (final int level : List.of(0, 10, 20)) {
            final var third = new Gear(build, EquipmentCatalogVersion.SCALING_V3).items(level);
            final var fourth = new Gear(build, EquipmentCatalogVersion.SCALING_V4).items(level);
            Assertions.assertAll(
                () -> Assertions.assertEquals(34, item(third, "ceremonial_tunic").object().speed()),
                () -> Assertions.assertEquals(24, item(third, "ceremonial_trousers").object().speed()),
                () -> Assertions.assertEquals(40, item(fourth, "ceremonial_tunic").object().speed()),
                () -> Assertions.assertEquals(30, item(fourth, "ceremonial_trousers").object().speed()),
                () -> Assertions.assertEquals(34, item(build.items(level), "ceremonial_tunic").object().speed())
            );
            for (int index = 0; index < third.size(); index++) {
                final var before = third.get(index);
                final var after = fourth.get(index);
                Assertions.assertAll(
                    () -> Assertions.assertEquals(before.object().code(), after.object().code()),
                    () -> Assertions.assertEquals(before.modifier(), after.modifier()),
                    () -> Assertions.assertEquals(before.rarity(), after.rarity()),
                    () -> Assertions.assertEquals(level, after.enhanceLevel()),
                    () -> Assertions.assertEquals(before.health(), after.health()),
                    () -> Assertions.assertEquals(before.itemAttacks(), after.itemAttacks()),
                    () -> Assertions.assertEquals(before.itemDefense(), after.itemDefense())
                );
            }
        }
    }

    @Test
    void removingSkillsPreservesRarityLevelAndAllItemStatistics() {
        final var build = Step12RepresentativeBuilds.role(RoleBuild.SKIRMISHER).withArmor("NEW_CLOTH");
        for (final var catalog : List.of(EquipmentCatalogVersion.SCALING_V3, EquipmentCatalogVersion.SCALING_V4)) {
            final var gear = new Gear(build, catalog);
            final var original = gear.items(10);
            final var disabled = gear.items(10, Set.of(ActiveEnum.HIT_AND_RUN));
            boolean foundCarrier = false;
            for (int index = 0; index < original.size(); index++) {
                final var before = original.get(index);
                final var after = disabled.get(index);
                Assertions.assertAll(
                    () -> Assertions.assertEquals(before.object(), after.object()),
                    () -> Assertions.assertEquals(before.rarity(), after.rarity()),
                    () -> Assertions.assertEquals(before.enhanceLevel(), after.enhanceLevel()),
                    () -> Assertions.assertEquals(before.health(), after.health()),
                    () -> Assertions.assertEquals(before.itemAttacks(), after.itemAttacks()),
                    () -> Assertions.assertEquals(before.itemDefense(), after.itemDefense())
                );
                if (before.modifier().isPresent()) {
                    foundCarrier = true;
                    Assertions.assertEquals(ActiveEnum.HIT_AND_RUN, before.modifier().orElseThrow().activeEnum());
                    Assertions.assertTrue(after.modifier().isEmpty());
                } else {
                    Assertions.assertEquals(before, after);
                }
            }
            Assertions.assertTrue(foundCarrier);
        }
    }

    @Test
    void timeoutRetainsTheSameObservationAsTheHistoricalRunner() {
        final var guardian = Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN);
        final var expected = Step12RepresentativeBattleRunner.observe(
            List.of(guardian), List.of(guardian), 10, 2_026_102_303L, true, 1, Set.of(), Set.of()
        );
        final var gear = new Gear(guardian, EquipmentCatalogVersion.SCALING_V3);
        final var actual = Step123BattleRunner.observe(
            List.of(gear), List.of(gear), 10, 2_026_102_303L, true, 1, Set.of(), Set.of()
        );
        Assertions.assertEquals("TIMEOUT", actual.outcome());
        Assertions.assertEquals(expected, actual);
    }

    private static Item item(List<Item> items, String code) {
        return items.stream().filter(item -> item.object().code().equals(code)).findFirst().orElseThrow();
    }
}
