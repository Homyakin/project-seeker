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
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.ResourceUtils;

/**
 * Slot availability and modifier type for the currently assignable part of the staged catalog.
 */
public final class ActiveSkillSlots {
    private static final String CATALOG_PATH = "game-data/item_modifiers_catalog.toml";
    private static final CatalogSnapshot CATALOG = load();

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
        return CATALOG.sortedSkills(slotFilters);
    }

    public static CatalogSnapshot legacySnapshot() {
        return CATALOG;
    }

    public static CatalogSnapshot snapshotForVersion(EquipmentCatalogVersion version) {
        final var catalog = EquipmentCatalogLoader.loadValidated(version);
        return snapshot(catalog.modifiers());
    }

    private static CatalogSnapshot load() {
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
        return new CatalogSnapshot(slotsBySkill, typesBySkill);
    }

    private static CatalogSnapshot snapshot(List<Modifier> modifiers) {
        final var slots = new EnumMap<ActiveEnum, Set<PersonageSlot>>(ActiveEnum.class);
        final var types = new EnumMap<ActiveEnum, ModifierType>(ActiveEnum.class);
        for (final var modifier : modifiers) {
            slots.put(modifier.activeEnum(), Set.copyOf(modifier.availableOnSlots()));
            types.put(modifier.activeEnum(), modifier.type());
        }
        return new CatalogSnapshot(slots, types);
    }

    public record CatalogSnapshot(
        Map<ActiveEnum, Set<PersonageSlot>> slotsBySkill,
        Map<ActiveEnum, ModifierType> typesBySkill
    ) {
        public CatalogSnapshot {
            slotsBySkill = slotsBySkill.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())
            ));
            typesBySkill = Map.copyOf(typesBySkill);
        }

        public Set<PersonageSlot> slotsFor(ActiveEnum skill) {
            return slotsBySkill.getOrDefault(skill, Set.of());
        }

        public ModifierType typeFor(ActiveEnum skill) {
            return typesBySkill.getOrDefault(skill, ModifierType.ANY);
        }

        public List<ActiveEnum> sortedSkills(Set<PersonageSlot> slotFilters) {
            return slotsBySkill.keySet().stream()
                .filter(skill -> slotsFor(skill).containsAll(slotFilters))
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        }
    }
}
