package ru.homyakin.seeker.game.battle.skill;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.ResourceUtils;

/**
 * Slot availability and modifier type for the currently assignable part of the staged catalog.
 */
public final class ActiveSkillSlots {
    private static final String CATALOG_PATH = "game-data/item_modifiers_catalog.toml";
    private static final CatalogData CATALOG = load();

    private ActiveSkillSlots() {
    }

    public static Set<PersonageSlot> slotsFor(ActiveEnum activeEnum) {
        return CATALOG.slotsBySkill().getOrDefault(activeEnum, Set.of());
    }

    public static ModifierType typeFor(ActiveEnum activeEnum) {
        return CATALOG.typesBySkill().getOrDefault(activeEnum, ModifierType.ANY);
    }

    public static List<ActiveEnum> sortedSkills() {
        return CATALOG.slotsBySkill().keySet().stream()
            .sorted(Comparator.comparing(Enum::name))
            .toList();
    }

    /**
     * Skills sorted by enum name. When {@code slotFilters} is non-empty, keeps only skills
     * available on every selected slot (AND).
     */
    public static List<ActiveEnum> sortedSkills(Set<PersonageSlot> slotFilters) {
        if (slotFilters.isEmpty()) {
            return sortedSkills();
        }
        return sortedSkills().stream()
            .filter(skill -> slotsFor(skill).containsAll(slotFilters))
            .toList();
    }

    private static CatalogData load() {
        final var slotsBySkill = new EnumMap<ActiveEnum, Set<PersonageSlot>>(ActiveEnum.class);
        final var typesBySkill = new EnumMap<ActiveEnum, ModifierType>(ActiveEnum.class);
        final var assignableCodes = ResourceUtils.calc(CATALOG_PATH, ItemModifiersToml::load)
            .orElseThrow(() -> new IllegalStateException("Active modifier catalog is missing"))
            .modifiers()
            .stream()
            .map(modifier -> modifier.code())
            .collect(Collectors.toUnmodifiableSet());
        final var stagedCatalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V1);
        for (final var modifier : stagedCatalog.modifiers()) {
            if (!assignableCodes.contains(modifier.code())) {
                continue;
            }
            slotsBySkill.put(modifier.activeEnum(), Set.copyOf(modifier.availableOnSlots()));
            typesBySkill.put(modifier.activeEnum(), modifier.type());
        }
        return new CatalogData(Map.copyOf(slotsBySkill), Map.copyOf(typesBySkill));
    }

    private record CatalogData(
        Map<ActiveEnum, Set<PersonageSlot>> slotsBySkill,
        Map<ActiveEnum, ModifierType> typesBySkill
    ) {
    }
}
