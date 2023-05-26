package ru.homyakin.seeker.locale.tavern_menu;

public record TavernMenuResource(
    String menuHeader,
    String drinks,
    String mainDishes,
    String itemNotInMenu,
    String[] notEnoughMoneyDrink,
    String[] notEnoughMoneyMainDish,
    String[] orderGiftToDifferentBot,
    String[] orderGiftToThisBot,
    String[] order,
    String[] orderGift,
    String consumeDrinkButton,
    String consumeMainDishButton,
    String consumeAlreadyInFinalStatus,
    String wrongConsumer,
    String[] expiredOrder
) {
}
