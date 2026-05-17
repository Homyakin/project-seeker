package ru.homyakin.seeker.game.item;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class EquippedCharacteristicsTest {
    @Test
    void givenNoEquippedItems_whenCalculate_thenUseAllDefaultItems() {
        final var expected = DefaultItems.characteristicsForFreeSlots(Set.of());

        Assertions.assertEquals(expected, calculate(List.of()));
    }

    @Test
    void givenEquippedItem_whenCalculate_thenSumItemAndRemainingDefaults() {
        final var shirt = personageItem(DefaultItems.SHIRT, true);
        final var expected = shirt.toItem().visibleCharacteristics()
            .add(DefaultItems.characteristicsForFreeSlots(shirt.object().slots()));

        Assertions.assertEquals(expected, calculate(List.of(shirt)));
    }

    @Test
    void givenEquippedMainFist_whenCalculate_thenDoNotAddMainFistDefault() {
        final var mainFist = personageItem(DefaultItems.MAIN_FIST, true);
        final var expected = mainFist.toItem().visibleCharacteristics()
            .add(DefaultItems.characteristicsForFreeSlots(mainFist.object().slots()));

        Assertions.assertEquals(expected, calculate(List.of(mainFist)));
    }

    @Test
    void givenOnlyMainHandOccupied_whenCalculate_thenAddOffFistDefault() {
        final var mainFist = personageItem(DefaultItems.MAIN_FIST, true);
        final var expected = mainFist.toItem().visibleCharacteristics()
            .add(DefaultItems.characteristicsForFreeSlots(mainFist.object().slots()));

        Assertions.assertEquals(expected, calculate(List.of(mainFist)));
    }

    private static Characteristics calculate(List<PersonageItem> equippedItems) {
        var total = equippedItems.stream()
            .map(PersonageItem::toItem)
            .map(item -> item.visibleCharacteristics())
            .reduce(Characteristics.ZERO, Characteristics::add);
        final var occupiedSlots = equippedItems.stream()
            .flatMap(item -> item.object().slots().stream())
            .collect(java.util.stream.Collectors.toSet());
        return total.add(DefaultItems.characteristicsForFreeSlots(occupiedSlots));
    }

    private static PersonageItem personageItem(ru.homyakin.seeker.game.item.models.Item item, boolean isEquipped) {
        return new PersonageItem(
            1L,
            1,
            item.object(),
            Optional.empty(),
            Optional.empty(),
            ItemRarity.COMMON,
            Optional.of(PersonageId.from(1L)),
            isEquipped
        );
    }
}
