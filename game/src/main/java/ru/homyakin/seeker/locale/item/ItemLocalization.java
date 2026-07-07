package ru.homyakin.seeker.locale.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.Inventory;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.models.PutOnItemResult;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ItemLocalization {
    private static final Resources<ItemResource> resources = new Resources<>();

    public static void add(Language language, ItemResource resource) {
        resources.add(language, resource);
    }

    public static String fullItem(Language requestedlanguage, PersonageItem item) {
        return fullItem(requestedlanguage, item, "");
    }

    public static String fullItem(Language requestedlanguage, PersonageItem item, String optionalCommand) {
        return fullItem(
            requestedlanguage,
            item,
            optionalCommand,
            item.object()
                .slots()
                .stream()
                .sorted(Comparator.comparingInt(it -> it.id))
                .map(it -> it.icon)
                .collect(Collectors.joining())
        );
    }

    private static String fullItem(
        Language requestedlanguage,
        PersonageItem item,
        String optionalCommand,
        String slots
    ) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", item.rarity().icon());
        params.put("broken_icon", "");
        params.put("item", itemText(itemLanguage, item));
        params.put("optional_command", optionalCommand);
        params.put("characteristics", itemCharacteristics(itemLanguage, item));
        params.put("slots", slots);
        return StringNamedTemplate.format(
            resources.getOrDefault(itemLanguage, ItemResource::fullItem),
            params
        );
    }

    public static String fullItem(Language requestedlanguage, Item item) {
        return fullItem(requestedlanguage, toDisplayItem(item));
    }

    public static String fullItemForShopSlot(Language requestedlanguage, PersonageItem item, PersonageSlot shopSlot) {
        return fullItemForShopSlot(requestedlanguage, item, shopSlot, "");
    }

    public static String fullItemForShopSlot(
        Language requestedlanguage,
        PersonageItem item,
        PersonageSlot shopSlot,
        String optionalCommand
    ) {
        return fullItem(
            requestedlanguage,
            item,
            optionalCommand,
            slotIcons(item, shopSlot)
        ).trim();
    }

    private static String slotIcons(PersonageItem item, PersonageSlot shopSlot) {
        final var fromObject = item.object().slots().stream()
            .sorted(Comparator.comparingInt(it -> it.id))
            .map(it -> it.icon)
            .collect(Collectors.joining());
        return fromObject.isEmpty() ? shopSlot.icon : fromObject;
    }

    public static String shortItem(Language requestedlanguage, PersonageItem item) {
        return shortItem(requestedlanguage, item, "");
    }

    public static String shortItem(Language requestedlanguage, PersonageItem item, String optionalCommand) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
        final var gameItem = item.toItem();
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", item.rarity().icon());
        params.put("broken_icon", "");
        params.put("item", itemText(itemLanguage, item));
        params.put("optional_command", optionalCommand);
        params.put("slots", itemSlots(item));
        params.put(
            "attack_type_icon",
            gameItem.itemAttack().map(attack -> Icons.attackTypeIcon(attack.attackType())).orElse("")
        );
        params.put(
            "defense_type_icon",
            gameItem.itemDefense().map(defense -> Icons.defenseTypeIcon(defense.defenseType())).orElse("")
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(itemLanguage, ItemResource::shortItem),
            params
        );
    }

    private static String itemSlots(PersonageItem item) {
        return item.object()
            .slots()
            .stream()
            .sorted(Comparator.comparingInt(it -> it.id))
            .map(it -> it.icon)
            .collect(Collectors.joining());
    }

    public static String equipment(Language language, Inventory inventory) {
        final var sortedItems = inventory.items().stream().sorted(ItemLocalization::itemComparator).toList();
        final var params = new HashMap<String, Object>();
        params.put(
            "equipped_items_and_free_slots",
            String.join("\n", buildEquipmentSlotLines(language, sortedItems, false))
        );
        params.put("battle_stats_command", CommandType.BATTLE_STATS.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::equipment),
            params
        );
    }

    public static String bag(Language language, Inventory inventory) {
        final var params = new HashMap<String, Object>();
        params.put("max_items_in_bag", Inventory.maxBagSize());
        final var itemsInBagBuilder = new StringBuilder();
        int itemsInBagCount = 0;
        final var sortedItems = inventory.items().stream().sorted(ItemLocalization::itemComparator).toList();
        for (final var item : sortedItems) {
            if (!item.isEquipped()) {
                itemsInBagBuilder.append(itemInBag(language, item)).append("\n");
                ++itemsInBagCount;
            }
        }
        params.put("items_in_bag_count", itemsInBagCount);
        params.put("items_in_bag", itemsInBagBuilder.toString());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::bag),
            params
        );
    }

    public static String compactInventory(Language language, Inventory inventory) {
        final var sortedItems = inventory.items().stream().sorted(ItemLocalization::itemComparator).toList();
        final var params = new HashMap<String, Object>();
        params.put(
            "equipped_items_and_free_slots",
            String.join("\n", buildEquipmentSlotLines(language, sortedItems, true))
        );
        params.put("max_items_in_bag", Inventory.maxBagSize());
        final var itemsInBagBuilder = new StringBuilder();
        int itemsInBagCount = 0;
        for (final var item : sortedItems) {
            if (!item.isEquipped()) {
                itemsInBagBuilder.append(shortItem(language, item, item.putOnCommand())).append("\n");
                ++itemsInBagCount;
            }
        }
        params.put("items_in_bag_count", itemsInBagCount);
        params.put("items_in_bag", itemsInBagBuilder.toString());
        params.put("battle_stats_command", CommandType.BATTLE_STATS.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::compactInventory),
            params
        );
    }

    public static String equipmentButton(Language language) {
        return resources.getOrDefault(language, ItemResource::equipmentButton);
    }

    public static String bagButton(Language language) {
        return resources.getOrDefault(language, ItemResource::bagButton);
    }

    public static int itemComparator(PersonageItem item1, PersonageItem item2) {
        return itemPriority(item1) - itemPriority(item2);
    }

    public static String personageMissingItem(Language language) {
        return resources.getOrDefault(language, ItemResource::personageMissingItem);
    }

    public static String alreadyEquipped(Language language) {
        return resources.getOrDefault(language, ItemResource::alreadyEquipped);
    }

    public static String alreadyTakenOff(Language language) {
        return resources.getOrDefault(language, ItemResource::alreadyTakenOff);
    }

    public static String notEnoughSpaceInBag(Language language) {
        return resources.getOrDefault(language, ItemResource::notEnoughSpaceInBag);
    }

    public static String notEnoughSpaceOnPutOnItem(Language language, List<PersonageSlot> slots) {
        final var slotsBuilder = new StringBuilder();
        for (final var slot : slots) {
            slotsBuilder.append(slot.icon);
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::notEnoughSpaceOnPutOnItem),
            Collections.singletonMap("slots", slotsBuilder.toString())
        );
    }

    public static String successPutOn(Language language, PutOnItemResult result) {
        final var params = new HashMap<String, Object>();
        params.put("item", fullItem(language, result.item()));
        if (result.takenOffItems().isEmpty()) {
            return StringNamedTemplate.format(
                resources.getOrDefault(language, ItemResource::successPutOn),
                params
            );
        }
        params.put(
            "taken_off_items",
            result.takenOffItems().stream()
                .map(item -> fullItem(language, item))
                .collect(Collectors.joining("\n"))
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::successPutOnWithTakenOff),
            params
        );
    }

    public static String successTakeOff(Language language, PersonageItem item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::successTakeOff),
            Collections.singletonMap("item", fullItem(language, item))
        );
    }

    private static String itemInBag(Language language, PersonageItem item) {
        return fullItem(language, item, item.putOnCommand());
    }

    private static String equippedItem(Language language, PersonageItem item) {
        return fullItem(language, item, item.takeOffCommand());
    }

    private static String personageFreeSlot(Language language, PersonageSlot slot) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::personageFreeSlot),
            Collections.singletonMap("slot", slot.icon)
        );
    }

    private static List<String> buildEquipmentSlotLines(
        Language language,
        List<PersonageItem> items,
        boolean compact
    ) {
        final var equipped = items.stream()
            .filter(PersonageItem::isEquipped)
            .sorted(ItemLocalization::itemComparator)
            .toList();
        final var occupiedSlots = equipped.stream()
            .flatMap(item -> item.object().slots().stream())
            .collect(Collectors.toSet());
        final var shownItemIds = new HashSet<Long>();
        final var shownDefaultItemCodes = new HashSet<String>();
        final var lines = new ArrayList<String>();

        Arrays.stream(PersonageSlot.values())
            .sorted(Comparator.comparingInt(ItemLocalization::slotPriority))
            .forEach(slot -> {
                if (occupiedSlots.contains(slot)) {
                    equipped.stream()
                        .filter(item -> item.object().slots().contains(slot))
                        .findFirst()
                        .ifPresent(item -> {
                            if (shownItemIds.add(item.id())) {
                                lines.add(
                                    compact
                                        ? shortItem(language, item, item.takeOffCommand())
                                        : equippedItem(language, item)
                                );
                            }
                        });
                } else {
                    DefaultItems.defaultItemForSlot(slot, occupiedSlots)
                        .ifPresentOrElse(
                            item -> {
                                final var code = item.object().code();
                                if (code == null || shownDefaultItemCodes.add(code)) {
                                    lines.add(
                                        compact ? shortItem(language, toDisplayItem(item)) : fullItem(language, item)
                                    );
                                }
                            },
                            () -> lines.add(personageFreeSlot(language, slot))
                        );
                }
            });
        return lines;
    }

    private static PersonageItem toDisplayItem(Item item) {
        return new PersonageItem(
            0L,
            0,
            item.object(),
            item.modifier().map(_ -> 0),
            item.modifier(),
            item.rarity(),
            Optional.empty(),
            true
        );
    }

    private static String itemCharacteristics(Language language, PersonageItem item) {
        return itemCharacteristics(language, item.toItem());
    }

    private static String itemCharacteristics(Language language, Item item) {
        final var params = new HashMap<String, Object>();
        putNotZeroCharacteristic(params, "not_zero_health", item.health(), health(language, item.health()));
        item.itemAttack().ifPresentOrElse(
            attack -> putNotZeroCharacteristic(
                params,
                "not_zero_attack",
                attack.attack(),
                attack(language, attack)
            ),
            () -> putEmptyCharacteristic(params, "not_zero_attack")
        );
        item.itemDefense().ifPresentOrElse(
            defense -> putNotZeroCharacteristic(
                params,
                "not_zero_defense",
                defense.defense(),
                defense(language, defense)
            ),
            () -> putEmptyCharacteristic(params, "not_zero_defense")
        );
        item.itemAttack().ifPresentOrElse(
            attack -> putNotZeroCharacteristic(
                params,
                "not_zero_range",
                attack.range(),
                range(language, attack.range())
            ),
            () -> putEmptyCharacteristic(params, "not_zero_range")
        );
        putNotZeroCharacteristic(
            params,
            "not_zero_speed",
            item.speed(), speed(language, item.speed())
        );
        putNotZeroCharacteristic(
            params,
            "not_zero_crit_chance",
            item.critChance(),
            critChance(language, item.critChance())
        );
        putNotZeroCharacteristic(
            params,
            "not_zero_dodge_chance",
            item.dodgeChance(),
            dodgeChance(language, item.dodgeChance())
        );
        putNotZeroCharacteristic(
            params,
            "not_zero_crit_multiplier",
            item.critMultiplier(),
            critMultiplier(language, item.critMultiplier())
        );
        putNotZeroCharacteristic(params, "not_zero_threat", item.baseThreat(), threat(language, item.baseThreat()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::characteristics),
            params
        );
    }

    private static void putNotZeroCharacteristic(
        HashMap<String, Object> params,
        String key,
        int value,
        String formatted
    ) {
        params.put(key, value != 0 ? formatted : "");
    }

    private static void putNotZeroCharacteristic(
        HashMap<String, Object> params,
        String key,
        double value,
        String formatted
    ) {
        params.put(key, value != 0 ? formatted : "");
    }

    private static void putEmptyCharacteristic(HashMap<String, Object> params, String key) {
        params.put(key, "");
    }

    private static String attack(Language language, ItemAttack attack) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::attack),
            Map.of(
                "attack_type_icon", Icons.attackTypeIcon(attack.attackType()),
                "attack_value", attack.attack()
            )
        );
    }

    private static String attack(Language language, int attack) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::attack),
            Map.of(
                "attack_type_icon", Icons.ATTACK,
                "attack_value", attack
            )
        );
    }

    private static String defense(Language language, ItemDefense defense) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::defense),
            Map.of(
                "defense_type_icon", Icons.defenseTypeIcon(defense.defenseType()),
                "defense_value", defense.defense()
            )
        );
    }

    private static String defense(Language language, int defense) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::defense),
            Map.of(
                "defense_type_icon", "",
                "defense_value", defense
            )
        );
    }

    private static String health(Language language, int health) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::health),
            Map.of("health_icon", Icons.HEALTH, "health_value", health)
        );
    }

    private static String range(Language language, int range) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::range),
            Map.of("range_icon", Icons.RANGE, "range_value", range)
        );
    }

    private static String speed(Language language, int speed) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::speed),
            Map.of("speed_icon", Icons.SPEED, "speed_value", speed)
        );
    }

    private static String critChance(Language language, int critChance) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::critChance),
            Map.of("crit_attack_icon", Icons.CRIT_ATTACK, "crit_chance_value", critChance)
        );
    }

    private static String dodgeChance(Language language, int dodgeChance) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::dodgeChance),
            Map.of("dodge_icon", Icons.DODGE, "dodge_chance_value", dodgeChance)
        );
    }

    private static String critMultiplier(Language language, double critMultiplier) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::critMultiplier),
            Map.of(
                "crit_multiplier_icon", Icons.CRIT_MULTIPLIER,
                "crit_multiplier_value", formatCritMultiplier(critMultiplier)
            )
        );
    }

    private static String threat(Language language, int threat) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::threat),
            Map.of("threat_icon", Icons.THREAT, "threat_value", threat)
        );
    }

    private static String formatCritMultiplier(double critMultiplier) {
        return String.format("%.2f", critMultiplier).replace(',', '.');
    }

    private static String itemText(Language itemLanguage, PersonageItem item) {
        final var params = new HashMap<String, Object>();
        final var objectLocale = item.object().getLocaleOrDefault(itemLanguage);
        params.put("object", objectLocale.text());
        params.put(
            "modifier",
            item.modifier()
                .map(it -> it.getLocaleOrDefault(itemLanguage).getFormOrWithout(objectLocale.form()) + " ")
                .orElse("")
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(itemLanguage, ItemResource::itemName),
            params
        );
    }

    private static int itemPriority(PersonageItem item) {
        return item.object().slots().stream().mapToInt(ItemLocalization::slotPriority).min().orElse(Integer.MAX_VALUE);
    }

    private static int slotPriority(PersonageSlot slot) {
        return switch (slot) {
            case MAIN_HAND -> 1;
            case OFF_HAND -> 2;
            case HELMET -> 3;
            case BODY -> 4;
            case GLOVES -> 5;
            case PANTS -> 6;
            case SHOES -> 7;
        };
    }
}
