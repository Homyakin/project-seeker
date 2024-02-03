package ru.homyakin.seeker.locale.tavern_menu;

import java.util.HashMap;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class TavernMenuLocalization {
    private static final Resources<TavernMenuResource> resources = new Resources<>();

    public static void add(Language language, TavernMenuResource resource) {
        resources.add(language, resource);
    }

    public static String menuHeader(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::menuHeader);
    }

    public static String drinks(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::drinks);
    }

    public static String itemNotInMenu(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::itemNotInMenu);
    }

    public static String mainDishes(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::mainDishes);
    }

    public static String notEnoughMoneyDrink(Language language, Money itemCost, Money personageMoneyValue) {
        return notEnoughMoney(
            resources.getOrDefaultRandom(language, TavernMenuResource::notEnoughMoneyDrink),
            itemCost,
            personageMoneyValue
        );
    }

    public static String notEnoughMoneyMainDish(Language language, Money itemCost, Money personageMoneyValue) {
        return notEnoughMoney(
            resources.getOrDefaultRandom(language, TavernMenuResource::notEnoughMoneyMainDish),
            itemCost,
            personageMoneyValue
        );
    }

    public static String orderGiftToDifferentBot(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::orderGiftToDifferentBot);
    }

    public static String orderGiftToThisBot(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::orderGiftToThisBot);
    }

    public static String order(Language language, MenuItem item, TgPersonageMention giver, TgPersonageMention acceptor) {
        final var params = new HashMap<String, Object>();
        params.put("item_name", item.name(language));
        params.put("mention_acceptor_icon_with_name", acceptor.value());
        final String text;
        if (!giver.equals(acceptor)) {
            params.put("mention_giver_icon_with_name", giver.value());
            text = resources.getOrDefaultRandom(language, TavernMenuResource::orderGift);
        } else {
            text = resources.getOrDefaultRandom(language, TavernMenuResource::order);
        }
        return StringNamedTemplate.format(text, params);
    }

    private static String notEnoughMoney(String template, Money itemCost, Money personageMoneyValue) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("not_enough_money_value", itemCost.value() - personageMoneyValue.value());
        params.put("item_cost", itemCost.value());
        return StringNamedTemplate.format(
            template,
            params
        );
    }

    public static String consumeDrinkButton(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::consumeDrinkButton);
    }

    public static String consumeMainDishButton(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::consumeMainDishButton);
    }

    public static String wrongConsumer(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::wrongConsumer);
    }

    public static String consumeAlreadyInFinalStatus(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::consumeAlreadyInFinalStatus);
    }

    public static String expiredOrder(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::expiredOrder);
    }

    public static String orderToUnknownUser(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::orderToUnknownUser);
    }

    public static String orderIsLocked(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::orderIsLocked);
    }
}
