package ru.homyakin.seeker.game.tavern_menu.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;

public enum Category {
    DRINK(1),
    MAIN_DISH(2),
    ;

    private final int id;

    Category(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getText(Language language) {
        return switch (this) {
            case DRINK -> TavernMenuLocalization.drinks(language);
            case MAIN_DISH -> TavernMenuLocalization.mainDishes(language);
        };
    }

    public static Category getById(int id) {
        for (Category category : values()) {
            if (category.getId() == id) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid Category id: " + id);
    }
}

