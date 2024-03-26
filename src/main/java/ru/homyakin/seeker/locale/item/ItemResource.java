package ru.homyakin.seeker.locale.item;

public record ItemResource(
    String itemWithoutModifiers,
    String itemWithPrefixModifier,
    String itemWithSuffixModifier,
    String itemWithPrefixAndSuffixModifier,
    String itemWithTwoPrefixModifiers,
    String characteristics,
    String attack,
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
    String successTakeOff
) {
}
