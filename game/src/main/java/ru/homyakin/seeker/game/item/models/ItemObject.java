package ru.homyakin.seeker.game.item.models;

import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ItemObject(
    String code,
    Set<PersonageSlot> slots,
    List<ItemAttack> attacks,
    Optional<ItemDefense> defense,
    int health,
    int critChance,
    int dodgeChance,
    double critMultiplier,
    int speed,
    int baseThreat,
    int impact,
    ItemProgressionVersion progressionVersion,
    Map<Language, ItemObjectLocale> locales
) implements Localized<ItemObjectLocale> {
    public ItemObject(
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
    ) {
        this(
            code,
            slots,
            attack.stream().toList(),
            defense,
            health,
            critChance,
            dodgeChance,
            critMultiplier,
            speed,
            baseThreat,
            0,
            ItemProgressionVersion.LEGACY,
            locales
        );
    }

    public ItemObject {
        slots = Set.copyOf(slots);
        attacks = List.copyOf(attacks);
        if (impact < 0) {
            throw new IllegalArgumentException("Impact contribution must be non-negative: " + impact);
        }
        progressionVersion = progressionVersion == null ? ItemProgressionVersion.LEGACY : progressionVersion;
    }

}
