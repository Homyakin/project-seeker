package ru.homyakin.seeker.game.item.catalog;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;

class ItemObjectsTomlCompatibilityTest {
    @Test
    void Given_LegacyAttack_When_LoadingCatalog_Then_MapsItToOneLegacyAttackPart() {
        final var object = loadSingleObject("""
            [[item]]
            code = "legacy_sword"
            slots = ["MAIN_HAND"]
            health = 0
            critChance = 2
            dodgeChance = 1
            critMultiplier = 0.05
            speed = 18
            baseThreat = 8

            [item.attack]
            attackType = "SLASH"
            range = 2
            attack = 300
            """);

        Assertions.assertEquals(
            List.of(new ItemAttack(AttackType.SLASH, 1, 2, 300)),
            object.attacks()
        );
        Assertions.assertEquals(0, object.impact());
        Assertions.assertEquals(ItemProgressionVersion.LEGACY, object.progressionVersion());
    }

    @Test
    void Given_AttackParts_When_LoadingCatalog_Then_PreservesRangesImpactAndProgressionVersion() {
        final var object = loadSingleObject("""
            [[item]]
            code = "composite_staff"
            slots = ["MAIN_HAND", "OFF_HAND"]
            health = 0
            critChance = 4
            dodgeChance = 1
            critMultiplier = 0.10
            speed = 24
            baseThreat = 4
            impact = 7
            progressionVersion = "V1"

            [[item.attacks]]
            attackType = "MAGICAL"
            minRange = 1
            maxRange = 4
            attack = 360

            [[item.attacks]]
            attackType = "MAGICAL"
            minRange = 3
            maxRange = 4
            attack = 120
            """);

        Assertions.assertEquals(
            List.of(
                new ItemAttack(AttackType.MAGICAL, 1, 4, 360),
                new ItemAttack(AttackType.MAGICAL, 3, 4, 120)
            ),
            object.attacks()
        );
        Assertions.assertEquals(7, object.impact());
        Assertions.assertEquals(ItemProgressionVersion.V1, object.progressionVersion());
    }

    @Test
    void Given_LegacyAndNewAttackForms_When_LoadingCatalog_Then_RejectsAmbiguousItem() {
        final var catalog = load("""
            [[item]]
            code = "ambiguous_weapon"
            slots = ["MAIN_HAND"]
            health = 0
            critChance = 0
            dodgeChance = 0
            critMultiplier = 0
            speed = 10
            baseThreat = 0

            [item.attack]
            attackType = "PIERCE"
            range = 3
            attack = 300

            [[item.attacks]]
            attackType = "PIERCE"
            minRange = 3
            maxRange = 3
            attack = 120
            """);

        final var exception = Assertions.assertThrows(IllegalArgumentException.class, catalog::itemObjects);
        Assertions.assertTrue(exception.getMessage().contains("either attack or attacks"));
    }

    private static ItemObject loadSingleObject(String toml) {
        final var objects = load(toml).itemObjects();
        Assertions.assertEquals(1, objects.size());
        return objects.getFirst();
    }

    private static ItemObjectsToml load(String toml) {
        try (final var stream = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8))) {
            return ItemObjectsToml.load(stream);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
