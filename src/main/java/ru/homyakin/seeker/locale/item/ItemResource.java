package ru.homyakin.seeker.locale.item;

public record ItemResource(
    String itemWithoutModifiers,
    String itemWithPrefixModifier,
    String itemWithSuffixModifier,
    String itemWithPrefixAndSuffixModifier,
    String itemWithTwoPrefixModifiers,
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
    String requiredFreeSlots,
    String successPutOn,
    String successTakeOff,
    String confirmDrop,
    String confirmDropButton,
    String rejectDropButton,
    String successDrop,
    String rejectedDrop
) {
}
