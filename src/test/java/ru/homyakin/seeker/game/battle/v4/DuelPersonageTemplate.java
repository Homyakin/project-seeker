package ru.homyakin.seeker.game.battle.v4;

public enum DuelPersonageTemplate {
    BASE(1000, 100, 100, 10, 20),
    COMMON(1100, 110, 110, 10, 20),
    UNCOMMON(1210, 121, 121, 10, 20),
    RARE(1330, 133, 133, 10, 20),
    EPIC(1460, 146, 146, 10, 20),
    LEGENDARY(1600, 160, 160, 10, 20),
    COMMON_LIGHT(440, 30, 8, 25, 20),
    COMMON_HEAVY(600, 28, 28, 0, 0),
    LEGENDARY_LIGHT(750, 45, 12, 25, 20),
    LEGENDARY_HEAVY(1020, 42, 42, 0, 0),
    LIGHT(450, 33, 8, 0, 20),
    HEAVY(600, 28, 28, 0, 0);

    private final int health;
    private final int attack;
    private final int defense;
    private final int critChance;
    private final int dodgeChance;

    DuelPersonageTemplate(
        int health,
        int attack,
        int defense,
        int critChance,
        int dodgeChance
    ) {
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.critChance = critChance;
        this.dodgeChance = dodgeChance;
    }

    public BattlePersonage create() {
        return new BattlePersonage(health, attack, defense, critChance, dodgeChance, 0, 0);
    }
}
