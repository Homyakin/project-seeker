package ru.homyakin.seeker.game.item.catalog;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.WordForm;

class ItemObjectRevisionPolicyTest {
    private final ItemObject sword = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V1)
        .itemObjects()
        .stream()
        .filter(item -> item.code().equals("sword"))
        .findFirst()
        .orElseThrow();

    @Test
    void identicalGameplayDoesNotInvalidateEnhancement() {
        final var renamed = copy(sword, sword.health(), Map.of(
            Language.RU,
            new ItemObjectLocale("Переименованный меч", WordForm.MASCULINE)
        ));

        Assertions.assertFalse(ItemObjectRevisionPolicy.invalidatesEnhancement(sword, renamed));
    }

    @Test
    void changedGameplayInvalidatesEnhancement() {
        final var changed = copy(sword, sword.health() + 60, sword.locales());

        Assertions.assertTrue(ItemObjectRevisionPolicy.invalidatesEnhancement(sword, changed));
    }

    @Test
    void differentCodesAreRejected() {
        final var changedCode = new ItemObject(
            "other-code",
            sword.slots(),
            sword.attacks(),
            sword.defense(),
            sword.health(),
            sword.critChance(),
            sword.dodgeChance(),
            sword.critMultiplier(),
            sword.speed(),
            sword.baseThreat(),
            sword.impact(),
            sword.progressionVersion(),
            sword.locales()
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ItemObjectRevisionPolicy.invalidatesEnhancement(sword, changedCode)
        );
    }

    private static ItemObject copy(
        ItemObject source,
        int health,
        Map<Language, ItemObjectLocale> locales
    ) {
        return new ItemObject(
            source.code(),
            source.slots(),
            source.attacks(),
            source.defense(),
            health,
            source.critChance(),
            source.dodgeChance(),
            source.critMultiplier(),
            source.speed(),
            source.baseThreat(),
            source.impact(),
            source.progressionVersion(),
            locales
        );
    }
}
