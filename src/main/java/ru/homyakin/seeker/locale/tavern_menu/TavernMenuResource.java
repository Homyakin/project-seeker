package ru.homyakin.seeker.locale.tavern_menu;

public record TavernMenuResource(
    String menuHeader,
    String drinks,
    String mainDishes,
    String itemNotInMenu,
    String[] notEnoughMoneyDrink,
    String[] notEnoughMoneyMainDish,
    String[] orderGiftToDifferentBot,
    String[] orderDrinkToThisBot,
    String[] orderMainDishToThisBot,
    String[] order,
    String[] orderGift,
    String consumeDrinkButton,
    String consumeMainDishButton,
    String consumeAlreadyInFinalStatus,
    String consumed,
    String wrongConsumer,
    String[] expiredOrder,
    String[] orderToUnknownUser,
    String orderIsLocked,
    String userNotFoundToThrow,
    String noOrdersToThrow,
    String[] onlyCreatedDish,
    String[] onlyCreatedDrink,
    String[] notEnoughMoneyToThrow,
    String throwResult,
    String throwEffect,
    String[] throwDishToNone,
    String[] throwDrinkToNone,
    String[] throwDishToPersonage,
    String[] throwDrinkToPersonage,
    String[] selfThrowDish,
    String[] selfThrowDrink,
    String[] throwToStaff
) {
}
