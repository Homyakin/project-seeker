package ru.homyakin.seeker.game.battle.v4;

import ru.homyakin.seeker.game.battle.v4.skill.ItemSkill;

import java.util.List;
import java.util.Optional;

public record Item(
    Optional<ItemAttack> itemAttack,
    Optional<ItemDefense> itemDefense,
    List<ItemSkill> itemSkills,
    int health
) {

    public static Item weapon(AttackType attackType, int range, int attack) {
        return new Item(
            Optional.of(new ItemAttack(attackType, range, attack)),
            Optional.empty(),
            List.of(),
            0
        );
    }

    public static Item armor(DefenseType defenseType, int defense, int health) {
        return new Item(
            Optional.empty(),
            Optional.of(new ItemDefense(defenseType, defense)),
            List.of(),
            health
        );
    }

    public static Item hybrid(
        AttackType attackType,
        int range,
        int attack,
        DefenseType defenseType,
        int defense,
        int health
    ) {
        return new Item(
            Optional.of(new ItemAttack(attackType, range, attack)),
            Optional.of(new ItemDefense(defenseType, defense)),
            List.of(),
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
