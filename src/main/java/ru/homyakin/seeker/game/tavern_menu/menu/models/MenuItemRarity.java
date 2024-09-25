package ru.homyakin.seeker.game.tavern_menu.menu.models;

public enum MenuItemRarity {
    COMMON(1, 1.0),
    UNCOMMON(2, 2.0),
    RARE(3, 3.0),
    ;

    private final int id;
    private final double effectMultiplier;

    MenuItemRarity(int id, double effectMultiplier) {
        this.id = id;
        this.effectMultiplier = effectMultiplier;
    }

    public int id() {
        return id;
    }

    public double effectMultiplier() {
        return effectMultiplier;
    }

    public static MenuItemRarity findById(int id) {
        for (MenuItemRarity rarity : values()) {
            if (rarity.id == id) {
                return rarity;
            }
        }
        throw new IllegalArgumentException("Invalid MenuItemRarity ID: " + id);
    }
}
