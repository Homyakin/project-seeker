package ru.homyakin.seeker.locale.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ItemLocalization {
    private static final Resources<ItemResource> resources = new Resources<>();

    public static void add(Language language, ItemResource resource) {
        resources.add(language, resource);
    }

    public static String fullItem(Language requestedlanguage, PersonageItem item) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", item.rarity().icon());
        params.put("broken_icon", "");
        params.put("item", itemText(itemLanguage, item));
        params.put("characteristics", itemCharacteristics(itemLanguage, item));
        params.put(
            "slots",
            item.object()
                .slots()
                .stream()
                .sorted(Comparator.comparingInt(it -> it.id))
                .map(it -> it.icon)
                .collect(Collectors.joining())
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(itemLanguage, ItemResource::fullItem),
            params
        );
    }

    public static String fullItem(Language requestedlanguage, Item item) {
        return fullItem(requestedlanguage, toDisplayItem(item));
    }

    public static String shortItem(Language requestedlanguage, PersonageItem item) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", item.rarity().icon());
        params.put("broken_icon", "");
        params.put("item", itemText(itemLanguage, item));
        params.put("characteristics", itemCharacteristics(itemLanguage, item));
        return StringNamedTemplate.format(
            resources.getOrDefault(itemLanguage, ItemResource::shortItem),
            params
        );
    }

    public static String shortItemWithoutCharacteristics(Language requestedlanguage, PersonageItem item) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", item.rarity().icon());
        params.put("broken_icon", "");
        params.put("item", itemText(itemLanguage, item));
        return StringNamedTemplate.format(
            resources.getOrDefault(itemLanguage, ItemResource::shortItemWithoutCharacteristics),
            params
        );
    }

    public static String inventory(Language language, Personage personage, List<PersonageItem> items) {
        final var params = new HashMap<String, Object>();
        params.put("max_items_in_bag", personage.maxBagSize());
        final var itemsInBagBuilder = new StringBuilder();
        int itemsInBagCount = 0;
        final var sortedItems = items.stream().sorted(ItemLocalization::itemComparator).toList();
        for (final var item : sortedItems) {
            if (!item.isEquipped()) {
                itemsInBagBuilder.append(itemInBag(language, item)).append("\n");
                ++itemsInBagCount;
            }
        }
        params.put(
            "equipped_items_and_free_slots",
            buildEquipmentSlotLines(language, sortedItems, true).stream()
                .collect(Collectors.joining("\n"))
        );
        params.put("items_in_bag_count", itemsInBagCount);
        params.put("items_in_bag", itemsInBagBuilder.toString());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::inventory),
            params
        );
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

    public static String requiredFreeSlots(Language language, List<PersonageSlot> slots) {
        final var builder = new StringBuilder();
        for (final var slot : slots) {
            builder.append(slot.icon);
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::requiredFreeSlots),
            Collections.singletonMap("busy_slots", builder.toString())
        );
    }

    public static String successPutOn(Language language, PersonageItem item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::successPutOn),
            Collections.singletonMap("item", fullItem(language, item))
        );
    }

    public static String successTakeOff(Language language, PersonageItem item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::successTakeOff),
            Collections.singletonMap("item", fullItem(language, item))
        );
    }

    private static String itemInBag(Language language, PersonageItem item) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", fullItem(language, item));
        params.put("put_on_command", item.putOnCommand());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemInBag),
            params
        );
    }

    private static String equippedItem(Language language, PersonageItem item) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", fullItem(language, item));
        params.put("take_off_command", item.takeOffCommand());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::equippedItem),
            params
        );
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
        boolean withCommands
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
                                lines.add(withCommands
                                    ? equippedItem(language, item)
                                    : fullItem(language, item));
                            }
                        });
                } else {
                    DefaultItems.defaultItemForSlot(slot, occupiedSlots)
                        .ifPresentOrElse(
                            item -> {
                                final var code = item.object().code();
                                if (code == null || shownDefaultItemCodes.add(code)) {
                                    lines.add(fullItem(language, item));
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

    private static String itemWithoutModifiers(Language language, PersonageItem item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithoutModifiers),
            Collections.singletonMap("object", item.object().getLocaleOrDefault(language).text())
        );
    }

    private static String itemWithModifier(Language language, PersonageItem item, Modifier modifier) {
        final var params = new HashMap<String, Object>();
        final var objectLocale = item.object().getLocaleOrDefault(language);
        params.put("object", objectLocale.text());
        params.put("prefix_modifier", modifier.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithPrefixModifier),
            params
        );
    }

    public static String characteristics(Language language, Characteristics characteristics) {
        final var params = new HashMap<String, Object>();
        if (characteristics.attack() != 0) {
            params.put("not_zero_attack", attack(language, characteristics.attack()));
        } else {
            params.put("not_zero_attack", "");
        }
        if (characteristics.health() != 0) {
            params.put("not_zero_health", health(language, characteristics.health()));
        } else {
            params.put("not_zero_health", "");
        }
        if (characteristics.defense() != 0) {
            params.put("not_zero_defense", defense(language, characteristics.defense()));
        } else {
            params.put("not_zero_defense", "");
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::characteristics),
            params
        );
    }

    private static String attack(Language language, int attack) {
        final var params = new HashMap<String, Object>();
        params.put("attack_icon", Icons.ATTACK);
        params.put("attack_value", attack);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::attack),
            params
        );
    }

    private static String health(Language language, int attack) {
        final var params = new HashMap<String, Object>();
        params.put("health_icon", Icons.HEALTH);
        params.put("health_value", attack);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::health),
            params
        );
    }

    private static String defense(Language language, int attack) {
        final var params = new HashMap<String, Object>();
        params.put("defense_icon", Icons.DEFENSE);
        params.put("defense_value", attack);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::defense),
            params
        );
    }

    private static String itemText(Language itemLanguage, PersonageItem item) {
        if (item.modifier().isEmpty()) {
            return itemWithoutModifiers(itemLanguage, item);
        }
        return itemWithModifier(itemLanguage, item, item.modifier().get());
    }

    private static String itemCharacteristics(Language language, PersonageItem item) {
        return characteristics(language, item.toItem().visibleCharacteristics());
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
