package ru.homyakin.seeker.game.item.models;

import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ItemObject(
    String code,
    Set<PersonageSlot> slots,
    Optional<ItemAttack> attack,
    Optional<ItemDefense> defense,
    int health,
    int critChance,
    int dodgeChance,
    double critMultiplier,
    int speed,
    int baseThreat,
    Map<Language, ItemObjectLocale> locales
) implements Localized<ItemObjectLocale> {
}
