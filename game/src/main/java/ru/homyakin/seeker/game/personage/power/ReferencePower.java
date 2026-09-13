package ru.homyakin.seeker.game.personage.power;

public record ReferencePower(
    double survivability,
    double normalDamagePerTime,
    double unscaled,
    double displayed
) {
}
