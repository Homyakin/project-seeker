package ru.homyakin.seeker.game.item.models;

import java.util.Optional;
import java.util.Set;
import ru.homyakin.seeker.game.item.storm.StormEnhanceConfig;
import ru.homyakin.seeker.game.personage.models.Characteristics;

public record Item(
    ItemObject object,
    Optional<Modifier> modifier,
    ItemRarity rarity,
    int enhanceLevel
) {
    public Item(ItemObject object, Optional<Modifier> modifier, ItemRarity rarity) {
        this(object, modifier, rarity, 0);
    }

    public Item withoutStormEnhance() {
        return enhanceLevel == 0 ? this : new Item(object, modifier, rarity, 0);
    }

    public Optional<ItemAttack> itemAttack() {
        return object.attack().map(attack -> new ItemAttack(
            attack.attackType(),
            attack.range(),
            applyEnhance(attack.attack())
        ));
    }

    public Optional<ItemDefense> itemDefense() {
        return object.defense().map(defense -> new ItemDefense(
            defense.defenseType(),
            applyEnhance(defense.defense())
        ));
    }

    public int health() {
        return applyEnhance(object.health());
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

    public Characteristics visibleCharacteristics() {
        final var attack = itemAttack().map(ItemAttack::attack).orElse(0);
        final var defense = itemDefense().map(ItemDefense::defense).orElse(0);
        return new Characteristics(health(), attack, defense);
    }

    private int applyEnhance(int base) {
        return StormEnhanceConfig.applyConfiguredBonus(base, enhanceLevel);
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
