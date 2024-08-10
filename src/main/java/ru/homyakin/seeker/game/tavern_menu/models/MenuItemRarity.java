package ru.homyakin.seeker.game.tavern_menu.models;

public enum MenuItemRarity {
    COMMON(1),
    UNCOMMON(2),
    RARE(3),
    ;

    private final int id;

    MenuItemRarity(int id) {
        this.id = id;
    }

    public int id() {
        return id;
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
