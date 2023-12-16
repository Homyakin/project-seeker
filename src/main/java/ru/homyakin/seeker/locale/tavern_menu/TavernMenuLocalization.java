package ru.homyakin.seeker.locale.tavern_menu;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class TavernMenuLocalization {
    private static final Map<Language, TavernMenuResource> map = new HashMap<>();

    public static void add(Language language, TavernMenuResource resource) {
        map.put(language, resource);
    }

    public static String menuHeader(Language language) {
        return CommonUtils.ifNullThan(map.get(language).menuHeader(), map.get(Language.DEFAULT).menuHeader());
    }

    public static String drinks(Language language) {
        return CommonUtils.ifNullThan(map.get(language).drinks(), map.get(Language.DEFAULT).drinks());
    }

    public static String itemNotInMenu(Language language) {
        return CommonUtils.ifNullThan(map.get(language).itemNotInMenu(), map.get(Language.DEFAULT).itemNotInMenu());
    }

    public static String mainDishes(Language language) {
        return CommonUtils.ifNullThan(map.get(language).mainDishes(), map.get(Language.DEFAULT).mainDishes());
    }

    public static String notEnoughMoneyDrink(Language language, Money itemCost, Money personageMoneyValue) {
        return notEnoughMoney(
            CommonUtils.ifNullThan(map.get(language).notEnoughMoneyDrink(), map.get(Language.DEFAULT).notEnoughMoneyDrink()),
            itemCost,
            personageMoneyValue
        );
    }

    public static String notEnoughMoneyMainDish(Language language, Money itemCost, Money personageMoneyValue) {
        return notEnoughMoney(
            CommonUtils.ifNullThan(map.get(language).notEnoughMoneyMainDish(), map.get(Language.DEFAULT).notEnoughMoneyMainDish()),
            itemCost,
            personageMoneyValue
        );
    }

    public static String orderGiftToDifferentBot(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(
                map.get(language).orderGiftToDifferentBot(),
                map.get(Language.DEFAULT).orderGiftToDifferentBot()
            )
        );
    }

    public static String orderGiftToThisBot(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(
                map.get(language).orderGiftToThisBot(),
                map.get(Language.DEFAULT).orderGiftToThisBot()
            )
        );
    }

    public static String order(Language language, MenuItem item, TgPersonageMention giver, TgPersonageMention acceptor) {
        final var params = new HashMap<String, Object>();
        params.put("item_name", item.name(language));
        params.put("mention_acceptor_icon_with_name", acceptor.value());
        final String text;
        if (giver.equals(acceptor)) {
            params.put("mention_giver_icon_with_name", giver.value());
            text = RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(map.get(language).orderGift(), map.get(Language.DEFAULT).orderGift())
            );

        } else {
            text = RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(map.get(language).order(), map.get(Language.DEFAULT).order())
            );
        }
        return StringNamedTemplate.format(text, params);
    }

    private static String notEnoughMoney(String[] array, Money itemCost, Money personageMoneyValue) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", TextConstants.MONEY_ICON);
        params.put("not_enough_money_value", itemCost.value() - personageMoneyValue.value());
        params.put("item_cost", itemCost.value());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(array),
            params
        );
    }

    public static String consumeDrinkButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).consumeDrinkButton(), map.get(Language.DEFAULT).consumeDrinkButton());
    }

    public static String consumeMainDishButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).consumeMainDishButton(), map.get(Language.DEFAULT).consumeMainDishButton());
    }

    public static String wrongConsumer(Language language) {
        return CommonUtils.ifNullThan(map.get(language).wrongConsumer(), map.get(Language.DEFAULT).wrongConsumer());
    }

    public static String consumeAlreadyInFinalStatus(Language language) {
        return CommonUtils.ifNullThan(
            map.get(language).consumeAlreadyInFinalStatus(), map.get(Language.DEFAULT).consumeAlreadyInFinalStatus()
        );
    }

    public static String expiredOrder(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).expiredOrder(), map.get(Language.DEFAULT).expiredOrder())
        );
    }

    public static String orderToUnknownUser(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).orderToUnknownUser(), map.get(Language.DEFAULT).orderToUnknownUser())
        );
    }

    public static String orderIsLocked(Language language) {
        return CommonUtils.ifNullThan(map.get(language).orderIsLocked(), map.get(Language.DEFAULT).orderIsLocked());
    }
}
