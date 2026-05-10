package ru.homyakin.seeker.game.battle.v4;

import java.util.Optional;

public record Item(
    Optional<ItemAttack> itemAttack,
    Optional<ItemDefense> itemDefense,
    Optional<Modifier> modifier,
    Rarity rarity,
    int health
) {

    public static Item weapon(
        AttackType attackType,
        int range,
        int attack,
        Modifier modifier,
        Rarity rarity
    ) {
        return new Item(
            Optional.of(new ItemAttack(attackType, range, attack)),
            Optional.empty(),
            Optional.of(modifier),
            rarity,
            0
        );
    }

    public static Item armor(
        DefenseType defenseType,
        int defense,
        int health,
        Modifier modifier,
        Rarity rarity
    ) {
        return new Item(
            Optional.empty(),
            Optional.of(new ItemDefense(defenseType, defense)),
            Optional.of(modifier),
            rarity,
            health
        );
    }

    public static Item hybrid(
        AttackType attackType,
        int range,
        int attack,
        DefenseType defenseType,
        int defense,
        int health,
        Modifier modifier,
        Rarity rarity
    ) {
        return new Item(
            Optional.of(new ItemAttack(attackType, range, attack)),
            Optional.of(new ItemDefense(defenseType, defense)),
            Optional.of(modifier),
            rarity,
            health
        );
    }

    public record ItemAttack(
        AttackType attackType,
        int range,
        int attack
    ) {
    }

    public record ItemDefense(
        DefenseType defenseType,
        int defense
    ) {
    }
}
