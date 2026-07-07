package ru.homyakin.seeker.locale.shop;

import ru.homyakin.seeker.game.item.models.Inventory;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
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
import java.util.Optional;
import java.util.stream.Collectors;

import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.item.models.CatalogItemObject;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public class ShopLocalization {
    private static final Resources<ShopResource> resources = new Resources<>();

    public static void add(Language language, ShopResource resource) {
        resources.add(language, resource);
    }

    public static String slotObjectsMenu(
        Language language,
        PersonageSlot slot,
        List<CatalogItemObject> objects,
        Money unitPrice,
        boolean compactItems
    ) {
        final var objectsText = objects.stream()
            .map(catalogObject -> slotObjectItem(
                language,
                slot,
                catalogObject,
                priceForObject(unitPrice, catalogObject.object()),
                compactItems
            ))
            .collect(Collectors.joining("\n"));
        final var params = new HashMap<String, Object>();
        params.put("slot_icon", slot.icon);
        params.put("objects", objectsText);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::slotObjectsMenu),
            params
        );
    }

    public static String randomBoxesButton(Language language) {
        return resources.getOrDefault(language, ShopResource::randomBoxesButton);
    }

    public static String menu(
        Language language,
        List<ShopItem> items,
        Optional<Contraband> activeContraband,
        boolean compactItems
    ) {
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
            selling.append(sellingItem(language, sellingItems.get(i), compactItems));
            if (i < sellingItems.size() - 1) {
                selling.append("\n");
            }
        }
        final var params = new HashMap<String, Object>();
        params.put("buying_items", buying.toString());
        params.put("selling_items", selling.toString());
        params.put("optional_contraband_notification", contrabandNotification(language, activeContraband));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::menu),
            params
        );
    }

    private static String contrabandNotification(Language language, Optional<Contraband> activeContraband) {
        if (activeContraband.isEmpty()) {
            return "";
        }
        final var params = new HashMap<String, Object>();
        params.put("contraband_command", CommandType.VIEW_CONTRABAND.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::contrabandNotification),
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

    public static String successBuy(Language language, PersonageItem item) {
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

    public static String enhanceTable(Language language, Inventory inventory) {
        final var params = new HashMap<String, Object>();
        final var sortedItems = inventory.items().stream().sorted(ItemLocalization::itemComparator).toList();
        final var equipped = sortedItems.stream()
                .filter(PersonageItem::isEquipped)
                .map(it -> enhanceItem(language, it))
                .collect(Collectors.joining("\n"));
        final var bagItems = sortedItems.stream()
                .filter(it -> !it.isEquipped())
                .map(it -> enhanceItem(language, it))
                .collect(Collectors.joining("\n"));
        params.put("equipped_enhance_times", equipped);
        params.put("inventory_enhance_times", bagItems);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::enhanceTable),
            params
        );
    }

    private static String enhanceItem(Language language, PersonageItem item) {
        final var params = new HashMap<String, Object>();
        params.put("item", ItemLocalization.shortItem(language, item));
        params.put("enhance_command", CommandType.ENHANCE_INFO.getText() + TextConstants.TG_COMMAND_DELIMITER + item.id());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::enhanceItem),
            params
        );
    }

    public static String enhanceItemInfo(Language language, AvailableAction action) {
        final var params = new HashMap<String, Object>();
        params.put("item", ItemLocalization.fullItem(language, action.item()));
        final String availableEnhance;
        if (action.action().isEmpty()) {
            availableEnhance = emptyEnhance(language);
        } else {
            availableEnhance = switch (action.action().get()) {
                case EnhanceAction.Enhance enhance -> enhance(language, action.item(), enhance.price());
            };
        }
        params.put("available_enhance", availableEnhance);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::enhanceItemInfo),
            params
        );
    }

    public static String maxRarity(Language language) {
        return resources.getOrDefault(language, ShopResource::maxModifiers);
    }

    public static String brokenItem(Language language) {
        return resources.getOrDefault(language, ShopResource::brokenItem);
    }

    public static String notBrokenItem(Language language) {
        return resources.getOrDefault(language, ShopResource::notBrokenItem);
    }

    public static String successAddModifier(Language language, AvailableAction action) {
        final var params = new HashMap<String, Object>();
        params.put("enhance_item_info", ShopLocalization.enhanceItemInfo(language, action));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::successAddModifier),
            params
        );
    }

    public static String successUpgradeRarity(Language language, AvailableAction action) {
        final var params = new HashMap<String, Object>();
        params.put("enhance_item_info", ShopLocalization.enhanceItemInfo(language, action));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::successUpgradeRarity),
            params
        );
    }

    private static String enhance(Language language, PersonageItem item, Money price) {
        final var params = new HashMap<String, Object>();
        params.put("price_value", price.value());
        params.put("money_icon", Icons.MONEY);
        params.put("enhance_command", CommandType.CONFIRM_ENHANCE.getText() + TextConstants.TG_COMMAND_DELIMITER + item.id());
        if (item.rarity() == ItemRarity.COMMON) {
            return StringNamedTemplate.format(
                resources.getOrDefault(language, ShopResource::addModifier),
                params
            );
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::upgradeRarity),
            params
        );
    }

    private static String emptyEnhance(Language language) {
        return resources.getOrDefault(language, ShopResource::emptyEnhance);
    }

    private static String sellingItem(Language language, ShopItem.Sell item, boolean compactItems) {
        final var sellCommand = CommandType.SELL_ITEM.getText() + TextConstants.TG_COMMAND_DELIMITER + item.item().id();
        final var params = new HashMap<String, Object>();
        params.put("item", shopItem(language, item.item(), sellCommand, compactItems));
        params.put("price_value", item.price().value());
        params.put("money_icon", Icons.MONEY);
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

    private static Money priceForObject(Money unitPrice, ItemObject object) {
        return Money.from(unitPrice.value() * Math.max(1, object.slots().size()));
    }

    private static String slotObjectItem(
        Language language,
        PersonageSlot slot,
        CatalogItemObject catalogObject,
        Money price,
        boolean compactItems
    ) {
        final var previewItem = new PersonageItem(
            0L,
            catalogObject.id(),
            catalogObject.object(),
            Optional.empty(),
            Optional.empty(),
            ItemRarity.COMMON,
            Optional.empty(),
            false
        );
        final var buyCommand = CommandType.BUY_ITEM.getText() + TextConstants.TG_COMMAND_DELIMITER + catalogObject.id();
        final var params = new HashMap<String, Object>();
        params.put("item", shopItemForSlot(language, previewItem, slot, buyCommand, compactItems));
        params.put("price_value", price.value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ShopResource::slotObjectItem),
            params
        ).trim();
    }

    private static String shopItem(
        Language language,
        PersonageItem item,
        String command,
        boolean compactItems
    ) {
        if (compactItems) {
            return ItemLocalization.shortItem(language, item, command);
        }
        return ItemLocalization.fullItem(language, item, command);
    }

    private static String shopItemForSlot(
        Language language,
        PersonageItem item,
        PersonageSlot slot,
        String command,
        boolean compactItems
    ) {
        if (compactItems) {
            return ItemLocalization.shortItem(language, item, command);
        }
        return ItemLocalization.fullItemForShopSlot(language, item, slot, command);
    }

}
