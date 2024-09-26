package ru.homyakin.seeker.locale.item;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.modifier.models.Modifier;
import ru.homyakin.seeker.game.item.modifier.models.ModifierType;
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

    public static String fullItem(Language requestedlanguage, Item item) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", item.rarity().icon);
        params.put("item", itemText(itemLanguage, item));
        params.put("characteristics", characteristics(itemLanguage, item.characteristics()));
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

    public static String shortItem(Language requestedlanguage, Item item) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", item.rarity().icon);
        params.put("item", itemText(itemLanguage, item));
        params.put("characteristics", characteristics(itemLanguage, item.characteristics()));
        return StringNamedTemplate.format(
            resources.getOrDefault(itemLanguage, ItemResource::shortItem),
            params
        );
    }

    public static String inventory(Language language, Personage personage, List<Item> items) {
        final var params = new HashMap<String, Object>();
        params.put("max_items_in_bag", personage.maxBagSize());
        final var itemsInBagBuilder = new StringBuilder();
        final var equippedItemsBuilder = new StringBuilder();
        int itemsInBagCount = 0;
        final var sortedItems = items.stream().sorted(Comparator.comparingInt(ItemLocalization::itemPriority)).toList();
        for (final var item : sortedItems) {
            if (item.isEquipped()) {
                if (!equippedItemsBuilder.isEmpty()) {
                    equippedItemsBuilder.append("\n");
                }
                equippedItemsBuilder.append(equippedItem(language, item));
            } else {
                itemsInBagBuilder.append(itemInBag(language, item)).append("\n");
                ++itemsInBagCount;
            }
        }
        final var freeSlots = personage.getFreeSlots(sortedItems)
            .stream()
            .sorted(Comparator.comparingInt(ItemLocalization::slotPriority))
            .map(slot -> personageFreeSlot(language, slot))
            .collect(Collectors.joining("\n"));
        if (equippedItemsBuilder.isEmpty()) {
            params.put("equipped_items_and_free_slots", freeSlots);
        } else if (freeSlots.isEmpty()) {
            params.put("equipped_items_and_free_slots", equippedItemsBuilder.toString());
        } else {
            params.put("equipped_items_and_free_slots", equippedItemsBuilder.append("\n").append(freeSlots).toString());
        }
        params.put("items_in_bag_count", itemsInBagCount);
        params.put("items_in_bag", itemsInBagBuilder.toString());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::inventory),
            params
        );
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

    public static String successPutOn(Language language, Item item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::successPutOn),
            Collections.singletonMap("item", fullItem(language, item))
        );
    }

    public static String successTakeOff(Language language, Item item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::successTakeOff),
            Collections.singletonMap("item", fullItem(language, item))
        );
    }

    public static String confirmDrop(Language language, Item item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::confirmDrop),
            Collections.singletonMap("item", fullItem(language, item))
        );
    }

    public static String confirmDropButton(Language language) {
        return resources.getOrDefault(language, ItemResource::confirmDropButton);
    }

    public static String rejectDropButton(Language language) {
        return resources.getOrDefault(language, ItemResource::rejectDropButton);
    }

    public static String successDrop(Language language, Item item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::successDrop),
            Collections.singletonMap("item", fullItem(language, item))
        );
    }

    public static String rejectedDrop(Language language) {
        return resources.getOrDefault(language, ItemResource::rejectedDrop);
    }

    private static String itemInBag(Language language, Item item) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", fullItem(language, item));
        params.put("put_on_command", item.putOnCommand());
        params.put("drop_command", item.dropCommand());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemInBag),
            params
        );
    }

    private static String equippedItem(Language language, Item item) {
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

    private static String itemWithoutModifiers(Language language, Item item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithoutModifiers),
            Collections.singletonMap("object", item.object().getLocaleOrDefault(language).text())
        );
    }

    private static String itemWithPrefixModifier(Language language, Item item, Modifier modifier) {
        final var params = new HashMap<String, Object>();
        final var objectLocale = item.object().getLocaleOrDefault(language);
        params.put("object", objectLocale.text());
        params.put("prefix_modifier", modifier.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithPrefixModifier),
            params
        );
    }

    private static String itemWithSuffixModifier(Language language, Item item, Modifier modifier) {
        final var params = new HashMap<String, Object>();
        final var objectLocale = item.object().getLocaleOrDefault(language);
        params.put("object", objectLocale.text());
        params.put("suffix_modifier", modifier.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithSuffixModifier),
            params
        );
    }

    private static String itemWithPrefixAndSuffixModifier(Language language, Item item, Modifier prefix, Modifier suffix) {
        final var params = new HashMap<String, Object>();
        final var objectLocale = item.object().getLocaleOrDefault(language);
        params.put("object", objectLocale.text());
        params.put("prefix_modifier", prefix.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        params.put("suffix_modifier", suffix.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithPrefixAndSuffixModifier),
            params
        );
    }

    private static String itemWithTwoPrefixModifiers(Language language, Item item, Modifier modifier1, Modifier modifier2) {
        final var params = new HashMap<String, Object>();
        final var objectLocale = item.object().getLocaleOrDefault(language);
        params.put("object", objectLocale.text());
        if (modifier1.id() > modifier2.id()) {
            params.put("prefix_modifier_one", modifier1.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
            params.put("prefix_modifier_two", modifier2.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        } else {
            params.put("prefix_modifier_one", modifier2.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
            params.put("prefix_modifier_two", modifier1.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithTwoPrefixModifiers),
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

    private static String itemText(Language itemLanguage, Item item) {
        if (item.modifiers().isEmpty()) {
            return itemWithoutModifiers(itemLanguage, item);
        } else if (item.modifiers().size() == 1) {
            final var modifier = item.modifiers().getFirst();
            return switch (modifier.type()) {
                case PREFIX -> itemWithPrefixModifier(itemLanguage, item, modifier);
                case SUFFIX -> itemWithSuffixModifier(itemLanguage, item, modifier);
            };
        } else if (item.modifiers().size() == 2) {
            final var modifier1 = item.modifiers().getFirst();
            final var modifier2 = item.modifiers().getLast();
            if (modifier1.type() == ModifierType.PREFIX && modifier2.type() == ModifierType.SUFFIX) {
                return itemWithPrefixAndSuffixModifier(itemLanguage, item, modifier1, modifier2);
            } else if (modifier1.type() == ModifierType.SUFFIX && modifier2.type() == ModifierType.PREFIX) {
                return itemWithPrefixAndSuffixModifier(itemLanguage, item, modifier2, modifier1);
            } else if (modifier1.type() == ModifierType.PREFIX && modifier2.type() == ModifierType.PREFIX) {
                return itemWithTwoPrefixModifiers(itemLanguage, item, modifier1, modifier2);
            }
        }
        return itemWithoutModifiers(itemLanguage, item);
    }

    private static int itemPriority(Item item) {
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
