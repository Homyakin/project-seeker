package ru.homyakin.seeker.locale.shop;

public record ShopResource(
    String menu,
    String sellingItem,
    String buyingItem,
    String common,
    String uncommon,
    String rare,
    String epic,
    String legendary,
    String random,
    String incorrectBuyingItem,
    String incorrectSellingItem,
    String notEnoughMoney,
    String notEnoughSpaceInBag,
    String successBuy,
    String successSell
) {
}
