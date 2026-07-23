package ru.homyakin.seeker.game.battle.skill;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.ResourceUtils;

/**
 * Slot availability for active skills from {@code item_modifiers_catalog.toml}.
 */
public final class ActiveSkillSlots {
    private static final String CATALOG_PATH = "game-data/item_modifiers_catalog.toml";
    private static final Map<ActiveEnum, Set<PersonageSlot>> SLOTS_BY_SKILL = load();

    private ActiveSkillSlots() {
    }

    public static Set<PersonageSlot> slotsFor(ActiveEnum activeEnum) {
        return SLOTS_BY_SKILL.getOrDefault(activeEnum, Set.of());
    }

    public static List<ActiveEnum> sortedSkills() {
        return Arrays.stream(ActiveEnum.values())
            .sorted(Comparator.comparing(Enum::name))
            .toList();
    }

    public static List<ActiveEnum> sortedSkills(Optional<PersonageSlot> slotFilter) {
        return sortedSkills().stream()
            .filter(skill -> slotFilter.isEmpty() || slotsFor(skill).contains(slotFilter.get()))
            .toList();
    }

    private static Map<ActiveEnum, Set<PersonageSlot>> load() {
        final var result = new EnumMap<ActiveEnum, Set<PersonageSlot>>(ActiveEnum.class);
        ResourceUtils.calc(CATALOG_PATH, ItemModifiersToml::load).ifPresent(catalog -> {
            for (final var modifier : catalog.modifiers()) {
                result.put(modifier.activeEnum(), Set.copyOf(modifier.availableOnSlots()));
            }
        });
        return Map.copyOf(result);
    }
}
