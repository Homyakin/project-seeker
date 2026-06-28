package ru.homyakin.seeker.locale.item;

public record ItemResource(
    String itemName,
    String fullItem,
    String shortItem,
    String shortItemWithoutCharacteristics,
    String characteristics,
    String attack,
    String health,
    String defense,
    String itemInBag,
    String equippedItem,
    String personageFreeSlot,
    String inventory,
    String personageMissingItem,
    String alreadyEquipped,
    String alreadyTakenOff,
    String notEnoughSpaceInBag,
    String notEnoughSpaceOnPutOnItem,
    String successPutOn,
    String successPutOnWithTakenOff,
    String successTakeOff
) {
}
