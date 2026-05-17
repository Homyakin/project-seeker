package ru.homyakin.seeker.game.item.models;

import java.util.Optional;
import java.util.Set;

public record Item(
    ItemObject object,
    Optional<Modifier> modifier,
    ItemRarity rarity
) {
    public Optional<ItemAttack> itemAttack() {
        return object.attack();
    }

    public Optional<ItemDefense> itemDefense() {
        return object.defense();
    }

    public int health() {
        return object.health();
    }

    public int critChance() {
        return object.critChance();
    }

    public int dodgeChance() {
        return object.dodgeChance();
    }

    public double critMultiplier() {
        return object.critMultiplier();
    }

    public int speed() {
        return object.speed();
    }

    public int baseThreat() {
        return object.baseThreat();
    }

    public int skillPoints() {
        return rarity.skillPoints() * object.slots().size();
    }

    public static Item weapon(
        AttackType attackType,
        int range,
        int attack,
        Modifier modifier,
        ItemRarity rarity
    ) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                Optional.of(new ItemAttack(attackType, range, attack)),
                Optional.empty(),
                0,
                0,
                0,
                0,
                0,
                0,
                null
            ),
            Optional.of(modifier),
            rarity
        );
    }

    public static Item armor(
        DefenseType defenseType,
        int defense,
        int health,
        Modifier modifier,
        ItemRarity rarity
    ) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                Optional.empty(),
                Optional.of(new ItemDefense(defenseType, defense)),
                health,
                0,
                0,
                0,
                0,
                0,
                null
            ),
            Optional.of(modifier),
            rarity
        );
    }

    public static Item stats(
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat
    ) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                0,
                critChance,
                dodgeChance,
                critMultiplier,
                speed,
                baseThreat,
                null
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    public static Item fromObject(ItemObject object) {
        return new Item(object, Optional.empty(), ItemRarity.COMMON);
    }

    public static Item hybrid(
        AttackType attackType,
        int range,
        int attack,
        DefenseType defenseType,
        int defense,
        int health,
        Modifier modifier,
        ItemRarity rarity
    ) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                Optional.of(new ItemAttack(attackType, range, attack)),
                Optional.of(new ItemDefense(defenseType, defense)),
                health,
                0,
                0,
                0,
                0,
                0,
                null
            ),
            Optional.of(modifier),
            rarity
        );
    }
}
