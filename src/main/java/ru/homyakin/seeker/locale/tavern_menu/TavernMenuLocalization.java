package ru.homyakin.seeker.locale.tavern_menu;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
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

    private static String notEnoughMoney(String[] array, Money itemCost, Money personageMoneyValue) {
        final var params = new HashMap<String, Object>() {{
            put("money_icon", TextConstants.MONEY_ICON);
            put("not_enough_money_value", itemCost.value() - personageMoneyValue.value());
            put("item_cost", itemCost.value());
        }};
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(array),
            params
        );
    }
}
