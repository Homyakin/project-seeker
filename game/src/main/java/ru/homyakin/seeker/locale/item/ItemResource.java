package ru.homyakin.seeker.locale.item;

public record ItemResource(
    String itemName,
    String fullItem,
    String shortItem,
    String characteristics,
    String attack,
    String health,
    String defense,
    String range,
    String speed,
    String critChance,
    String dodgeChance,
    String critMultiplier,
    String threat,
    String personageFreeSlot,
    String equipment,
    String bag,
    String equipmentButton,
    String bagButton,
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
