package ru.homyakin.seeker.locale.item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
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

    public static String item(Language requestedlanguage, Item item) {
        final var itemLanguage = item.getItemLanguage(requestedlanguage);
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

    private static String itemWithoutModifiers(Language language, Item item) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithoutModifiers),
            requiredItemParams(language, item)
        );
    }

    private static String itemWithPrefixModifier(Language language, Item item, Modifier modifier) {
        final var params = requiredItemParams(language, item);
        final var objectLocale = item.object().getLocaleOrDefault(language);
        params.put("prefix_modifier", modifier.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithPrefixModifier),
            params
        );
    }

    private static String itemWithSuffixModifier(Language language, Item item, Modifier modifier) {
        final var params = requiredItemParams(language, item);
        final var objectLocale = item.object().getLocaleOrDefault(language);
        params.put("suffix_modifier", modifier.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithSuffixModifier),
            params
        );
    }

    private static String itemWithPrefixAndSuffixModifier(Language language, Item item, Modifier prefix, Modifier suffix) {
        final var params = requiredItemParams(language, item);
        final var objectLocale = item.object().getLocaleOrDefault(language);
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
        params.put("item", objectLocale.text());
        if (modifier1.id() > modifier2.id()) {
            params.put("prefix_modifier_one", modifier1.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
            params.put("prefix_modifier_two", modifier2.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        } else  {
            params.put("prefix_modifier_one", modifier2.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
            params.put("prefix_modifier_two", modifier1.getLocaleOrDefault(language).getFormOrWithout(objectLocale.form()));
        }
        params.put("characteristics", characteristics(language, item));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ItemResource::itemWithTwoPrefixModifiers),
            params
        );
    }

    private static String characteristics(Language language, Item item) {
        final var params = new HashMap<String, Object>();
        if (item.characteristics().attack() != 0) {
            params.put("not_zero_attack", attack(language, item.characteristics().attack()));
        } else {
            params.put("not_zero_attack", "");
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

    private static Map<String, Object> requiredItemParams(Language language, Item item) {
        final var params = new HashMap<String, Object>();
        params.put("item", item.object().getLocaleOrDefault(language).text());
        params.put("characteristics", characteristics(language, item));
        params.put(
            "slots",
            item.object()
                .slots()
                .stream()
                .sorted(Comparator.comparingInt(it -> it.id))
                .map(it -> it.icon)
                .collect(Collectors.joining())
        );
        return params;
    }
}
