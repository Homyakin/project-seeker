package ru.homyakin.seeker.locale.shop;

import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.shop.models.AvailableAction;
import ru.homyakin.seeker.game.shop.models.EnhanceAction;
import ru.homyakin.seeker.game.shop.models.ShopItem;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
import ru.homyakin.seeker.game.shop.models.SoldItem;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ShopLocalization {
    private static final Resources<ShopResource> resources = new Resources<>();

    public static void add(Language language, ShopResource resource) {
        resources.add(language, resource);
    }

    public static String menu(Language language, List<ShopItem> items) {
        final var buying = new StringBuilder();
        final var selling = new StringBuilder();
        final var buyingItems = items.stream()
            .filter(item -> item instanceof ShopItem.Buy)
            .map(item -> (ShopItem.Buy) item)
            .toList();
        final var sellingItems = items.stream()
            .filter(item -> item instanceof ShopItem.Sell)
            .map(item -> (ShopItem.Sell) item)
            .sorted((item1, item2) -> ItemLocalization.itemComparator(item1.item(), item2.item()))
            .toList();
        for (int i = 0; i < buyingItems.size(); ++i) {
            buying.append(buyingItem(language, buyingItems.get(i)));
            if (i < buyingItems.size() - 1) {
                buying.append("\n");
            }
        }
        for (int i = 0; i < sellingItems.size(); ++i) {
            selling.append(sellingItem(language, sellingItems.get(i)));
            if (i < sellingItems.size() - 1) {
                selling.append("\n");
            }
        }
        final var params = new HashMap<String, Object>();
        params.put("buying_items", buying.toString());
        params.put("selling_items", selling.toString());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::menu),
            params
        );
    }

    public static String incorrectBuyingItem(Language language) {
        return resources.getOrDefault(language, ShopResource::incorrectBuyingItem);
    }

    public static String noItemAtPersonage(Language language) {
        return resources.getOrDefault(language, ShopResource::noItemAtPersonage);
    }

    public static String notEnoughMoney(Language language, Money required) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("required_value", required.value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::notEnoughMoney),
            params
        );
    }

    public static String notEnoughSpaceInBag(Language language) {
        return resources.getOrDefault(language, ShopResource::notEnoughSpaceInBag);
    }

    public static String successBuy(Language language, Item item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::successBuy),
            Collections.singletonMap("full_item_name", ItemLocalization.fullItem(language, item))
        );
    }

    public static String successSell(Language language, SoldItem item) {
        final var params = new HashMap<String, Object>();
        params.put("full_item_name", ItemLocalization.fullItem(language, item.item()));
        params.put("price_value", item.price().value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::successSell),
            params
        );
    }

    public static String enhanceTable(Language language, List<Item> items) {
        final var params = new HashMap<String, Object>();
        final var sortedItems = items.stream().sorted(ItemLocalization::itemComparator).toList();
        final var equipped = sortedItems.stream()
                .filter(Item::isEquipped)
                .map(it -> enhanceItem(language, it))
                .collect(Collectors.joining("\n"));
        final var inventory = sortedItems.stream()
                .filter(it -> !it.isEquipped())
                .map(it -> enhanceItem(language, it))
                .collect(Collectors.joining("\n"));
        params.put("equipped_enhance_times", equipped);
        params.put("inventory_enhance_times", inventory);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::enhanceTable),
            params
        );
    }

    private static String enhanceItem(Language language, Item item) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", ItemLocalization.fullItem(language, item));
        params.put("enhance_command", CommandType.ENHANCE_INFO.getText() + TextConstants.TG_COMMAND_DELIMITER + item.id());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::enhanceItem),
            params
        );
    }

    public static String enhanceItemInfo(Language language, AvailableAction action) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", ItemLocalization.fullItem(language, action.item()));
        final String availableEnhance;
        if (action.action().isEmpty()) {
            availableEnhance = emptyEnhance(language);
        } else {
            availableEnhance = switch (action.action().get()) {
                case EnhanceAction.AddModifier addModifier -> addModifier(language, action.item(), addModifier.price());
            };
        }
        params.put("available_enhance", availableEnhance);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::enhanceItemInfo),
            params
        );
    }

    public static String maxModifiers(Language language) {
        return resources.getOrDefault(language, ShopResource::maxModifiers);
    }

    public static String successAddModifier(Language language, AvailableAction action) {
        final var params = new HashMap<String, Object>();
        params.put("enhance_item_info", ShopLocalization.enhanceItemInfo(language, action));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::successAddModifier),
            params
        );
    }

    private static String addModifier(Language language, Item item, Money price) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", ItemLocalization.fullItem(language, item));
        params.put("price_value", price.value());
        params.put("money_icon", Icons.MONEY);
        params.put("add_modifier_command", CommandType.ADD_MODIFIER.getText() + TextConstants.TG_COMMAND_DELIMITER + item.id());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::addModifier),
            params
        );
    }

    private static String emptyEnhance(Language language) {
        return resources.getOrDefault(language, ShopResource::emptyEnhance);
    }

    private static String sellingItem(Language language, ShopItem.Sell item) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", ItemLocalization.fullItem(language, item.item()));
        params.put("price_value", item.price().value());
        params.put("money_icon", Icons.MONEY);
        params.put("sell_command", CommandType.SELL_ITEM.getText() + TextConstants.TG_COMMAND_DELIMITER + item.item().id());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::sellingItem),
            params
        );
    }

    private static String buyingItem(Language language, ShopItem.Buy item) {
        final var params = new HashMap<String, Object>();
        params.put("shop_item_icon", item.type().icon);
        params.put("shop_item_name", shopItemName(language, item.type()));
        params.put("price_value", item.price().value());
        params.put("money_icon", Icons.MONEY);
        params.put("buy_command", CommandType.BUY_ITEM.getText() + TextConstants.TG_COMMAND_DELIMITER + item.type().telegramCode);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::buyingItem),
            params
        );
    }

    private static String shopItemName(Language language, ShopItemType type) {
        return switch (type) {
            case COMMON -> resources.getOrDefault(language, ShopResource::common);
            case UNCOMMON -> resources.getOrDefault(language, ShopResource::uncommon);
            case RARE -> resources.getOrDefault(language, ShopResource::rare);
            case EPIC -> resources.getOrDefault(language, ShopResource::epic);
            case LEGENDARY -> resources.getOrDefault(language, ShopResource::legendary);
            case RANDOM -> resources.getOrDefault(language, ShopResource::random);
        };
    }

}
