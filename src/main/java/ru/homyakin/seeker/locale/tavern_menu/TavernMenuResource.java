package ru.homyakin.seeker.locale.tavern_menu;

public record TavernMenuResource(
    String menuHeader,
    String drinks,
    String mainDishes,
    String itemNotInMenu,
    String[] notEnoughMoneyDrink,
    String[] notEnoughMoneyMainDish
) {
}
