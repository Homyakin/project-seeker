package ru.homyakin.seeker.game.personage.power;

/** Personal equipment assessment before any presentation scale or rounding. */
public record EquipmentPower(
    double survivability,
    double normalDamagePerTime,
    double unscaled
) {
}
