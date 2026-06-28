package ru.homyakin.seeker.game.item.models;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.test_utils.CatalogTestUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;

class InventoryPutOnItemTest {
    @Test
    void canPutOnTwoHandedWhenOneHandEquippedAndBagIsFull() {
        final var personageId = PersonageUtils.random().id();
        final var oneHandEquipped = personageItem(
            1L,
            personageId,
            true,
            PersonageSlot.MAIN_HAND
        );
        final var twoHandedInBag = personageItem(
            2L,
            personageId,
            false,
            PersonageSlot.MAIN_HAND,
            PersonageSlot.OFF_HAND
        );
        final var inventoryItems = new ArrayList<PersonageItem>();
        inventoryItems.add(oneHandEquipped);
        inventoryItems.add(twoHandedInBag);
        for (long id = 3; id <= Inventory.maxBagSize() + 1; ++id) {
            inventoryItems.add(personageItem(id, personageId, false, PersonageSlot.BODY));
        }

        final var result = new Inventory(inventoryItems).canPutOnItem(personageId, twoHandedInBag);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(List.of(oneHandEquipped), result.get());
    }

    @Test
    void cannotPutOnTwoHandedWhenBothHandsEquippedAndBagIsFull() {
        final var personageId = PersonageUtils.random().id();
        final var mainHandEquipped = personageItem(
            1L,
            personageId,
            true,
            PersonageSlot.MAIN_HAND
        );
        final var offHandEquipped = personageItem(
            2L,
            personageId,
            true,
            PersonageSlot.OFF_HAND
        );
        final var twoHandedInBag = personageItem(
            3L,
            personageId,
            false,
            PersonageSlot.MAIN_HAND,
            PersonageSlot.OFF_HAND
        );
        final var inventoryItems = new ArrayList<PersonageItem>();
        inventoryItems.add(mainHandEquipped);
        inventoryItems.add(offHandEquipped);
        inventoryItems.add(twoHandedInBag);
        for (long id = 4; id <= Inventory.maxBagSize() + 2; ++id) {
            inventoryItems.add(personageItem(id, personageId, false, PersonageSlot.BODY));
        }

        final var result = new Inventory(inventoryItems).canPutOnItem(personageId, twoHandedInBag);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(
            new PutOnItemError.NotEnoughSpaceOnPutOnItem(List.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND)),
            result.getLeft()
        );
    }

    @Test
    void canPutOnItemWhenRequiredSlotsAreFree() {
        final var personageId = PersonageUtils.random().id();
        final var itemInBag = personageItem(1L, personageId, false, PersonageSlot.MAIN_HAND);

        final var result = new Inventory(List.of(itemInBag)).canPutOnItem(personageId, itemInBag);

        Assertions.assertTrue(result.isRight());
        Assertions.assertTrue(result.get().isEmpty());
    }

    private PersonageItem personageItem(
        long id,
        PersonageId personageId,
        boolean isEquipped,
        PersonageSlot... slots
    ) {
        return new PersonageItem(
            id,
            1,
            CatalogTestUtils.itemObject(slots),
            Optional.empty(),
            Optional.empty(),
            ItemRarity.COMMON,
            Optional.of(personageId),
            isEquipped
        );
    }
}
