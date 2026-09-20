package ru.homyakin.seeker.game.item.catalog;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.Characteristics;

class ScalingV2ExistingItemMigrationTest {
    private static final Map<String, ItemObject> PREVIOUS = loadPrevious();
    private static final Map<String, ItemObject> REPLACEMENT = EquipmentCatalogLoader
        .loadValidated(EquipmentCatalogVersion.SCALING_V2)
        .itemObjects()
        .stream()
        .collect(Collectors.toUnmodifiableMap(ItemObject::code, Function.identity()));

    @Test
    void everyExistingCodeCanBeRecalculatedAtSameLevel() {
        Assertions.assertEquals(40, PREVIOUS.size());
        for (final var entry : PREVIOUS.entrySet()) {
            final var replacement = REPLACEMENT.get(entry.getKey());
            Assertions.assertNotNull(replacement, entry.getKey());
            for (int level = 0; level <= 75; level++) {
                final var previousItem = item(entry.getValue(), level);
                final var replacementItem = item(replacement, level);
                Assertions.assertEquals(level, previousItem.enhanceLevel());
                Assertions.assertEquals(level, replacementItem.enhanceLevel());
                Assertions.assertDoesNotThrow(previousItem::visibleCharacteristics);
                Assertions.assertDoesNotThrow(replacementItem::visibleCharacteristics);
            }
        }
    }

    @Test
    void controlLevelComparisonIsExplicit() {
        Assertions.assertAll(
            () -> assertComparison(
                0, new Comparison(22, 1, 1), new Comparison(12, 0, 4), new Comparison(24, 0, 0)
            ),
            () -> assertComparison(
                3, new Comparison(20, 0, 4), new Comparison(12, 0, 4), new Comparison(24, 0, 0)
            ),
            () -> assertComparison(
                6, new Comparison(19, 0, 5), new Comparison(9, 0, 7), new Comparison(24, 0, 0)
            ),
            () -> assertComparison(
                10, new Comparison(11, 5, 8), new Comparison(4, 0, 12), new Comparison(24, 0, 0)
            ),
            () -> assertComparison(
                20, new Comparison(1, 7, 16), new Comparison(4, 0, 12), new Comparison(24, 0, 0)
            ),
            () -> assertComparison(
                75, new Comparison(0, 0, 24), new Comparison(4, 0, 12), new Comparison(24, 0, 0)
            )
        );
    }

    @Test
    void knownDecreasesAndPreviousSmallBaseRemainExplicit() {
        Assertions.assertEquals(1200, item(PREVIOUS.get("breastplate"), 20).health());
        Assertions.assertEquals(800, item(REPLACEMENT.get("breastplate"), 20).health());
        Assertions.assertEquals(796, visible(PREVIOUS.get("halberd"), 20).attack());
        Assertions.assertEquals(480, visible(REPLACEMENT.get("halberd"), 20).attack());
        Assertions.assertEquals(44, visible(PREVIOUS.get("dagger"), 1).attack());
        Assertions.assertEquals(122, visible(REPLACEMENT.get("dagger"), 1).attack());
    }

    private static void assertComparison(
        int level,
        Comparison expectedHealth,
        Comparison expectedAttack,
        Comparison expectedDefense
    ) {
        var health = Comparison.ZERO;
        var attack = Comparison.ZERO;
        var defense = Comparison.ZERO;
        for (final var entry : PREVIOUS.entrySet()) {
            final var previous = visible(entry.getValue(), level);
            final var replacement = visible(REPLACEMENT.get(entry.getKey()), level);
            if (previous.health() != 0 || replacement.health() != 0) {
                health = health.add(previous.health(), replacement.health());
            }
            if (previous.attack() != 0 || replacement.attack() != 0) {
                attack = attack.add(previous.attack(), replacement.attack());
            }
            if (previous.defense() != 0 || replacement.defense() != 0) {
                defense = defense.add(previous.defense(), replacement.defense());
            }
        }
        final var actualHealth = health;
        final var actualAttack = attack;
        final var actualDefense = defense;
        Assertions.assertAll(
            () -> Assertions.assertEquals(expectedHealth, actualHealth, "health/level=" + level),
            () -> Assertions.assertEquals(expectedAttack, actualAttack, "attack/level=" + level),
            () -> Assertions.assertEquals(expectedDefense, actualDefense, "defense/level=" + level)
        );
    }

    private static Characteristics visible(ItemObject object, int level) {
        return item(object, level).visibleCharacteristics();
    }

    private static Item item(ItemObject object, int level) {
        return new Item(object, Optional.empty(), ItemRarity.COMMON, level);
    }

    private static Map<String, ItemObject> loadPrevious() {
        try (final var stream = ScalingV2ExistingItemMigrationTest.class
            .getClassLoader()
            .getResourceAsStream("game-data/item_objects_catalog.toml")) {
            if (stream == null) {
                throw new IllegalStateException("Previous item catalog is missing");
            }
            return ItemObjectsToml.load(stream).itemObjects().stream()
                .collect(Collectors.toUnmodifiableMap(ItemObject::code, Function.identity()));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to close previous item catalog", e);
        }
    }

    private record Comparison(int gains, int equals, int losses) {
        private static final Comparison ZERO = new Comparison(0, 0, 0);

        private Comparison add(int previous, int replacement) {
            if (replacement > previous) {
                return new Comparison(gains + 1, equals, losses);
            }
            if (replacement < previous) {
                return new Comparison(gains, equals, losses + 1);
            }
            return new Comparison(gains, equals + 1, losses);
        }
    }
}
