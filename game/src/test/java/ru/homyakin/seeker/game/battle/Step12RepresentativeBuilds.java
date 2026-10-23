package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.battle.simulation.Step12ScalingV3Fixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

/** Legal catalog equipment for the representative battles required by step 12.2. */
final class Step12RepresentativeBuilds {
    private static final Map<String, ItemObject> OBJECTS;
    private static final Map<ActiveEnum, Modifier> MODIFIERS;

    static {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        OBJECTS = catalog.itemObjects().stream().collect(Collectors.toUnmodifiableMap(
            ItemObject::code,
            Function.identity()
        ));
        MODIFIERS = catalog.modifiers().stream().collect(Collectors.toUnmodifiableMap(
            Modifier::activeEnum,
            Function.identity()
        ));
    }

    private Step12RepresentativeBuilds() {
    }

    static Build role(RoleBuild role) {
        return fromItems(
            role.name(),
            Step12ScalingV3Fixtures.items(role, 0),
            role.position(),
            role.targetingTactic()
        );
    }

    static Build mixed(MixedBuild mixed) {
        return fromItems(
            mixed.name(),
            Step12ScalingV3Fixtures.items(mixed, 0),
            mixed.position(),
            mixed.targetingTactic()
        );
    }

    static List<List<String>> handBlocks() {
        final var result = new ArrayList<List<String>>();
        for (final var main : List.of("sword", "rapier", "mace", "spear", "staff")) {
            for (final var off : List.of(
                "shortsword", "club", "dagger", "dirk", "orb",
                "buckler", "shield", "tower_shield", "tome"
            )) {
                result.add(List.of(main, off));
            }
        }
        for (final var both : List.of(
            "bow", "longbow", "crossbow", "two_handed_sword", "sledgehammer",
            "halberd", "battle_staff", "ritual_staff", "command_staff"
        )) {
            result.add(List.of(both));
        }
        return List.copyOf(result);
    }

    static List<ArmorPair> armorPairs() {
        final var result = new ArrayList<ArmorPair>();
        for (final var family : List.of("CLOTH", "LEATHER", "PLATE", "ARCANE")) {
            final var oldArmor = armor(family);
            final var newArmor = armor("NEW_" + family);
            for (int index = 0; index < oldArmor.size(); index++) {
                result.add(new ArmorPair(oldArmor.get(index), newArmor.get(index), family));
            }
        }
        return List.copyOf(result);
    }

    /** Body, pants, shoes, helmet and gloves, in that order. */
    static List<String> armor(String family) {
        return switch (family) {
            case "CLOTH" -> List.of("robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves");
            case "LEATHER" -> List.of("breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves");
            case "PLATE" -> List.of("cuirass", "greaves", "sabatons", "great_helm", "gauntlets");
            case "ARCANE" -> List.of("wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves");
            case "NEW_CLOTH" -> List.of(
                "ceremonial_tunic", "ceremonial_trousers", "ceremonial_slippers", "ceremonial_veil", "ceremonial_gloves"
            );
            case "NEW_LEATHER" -> List.of(
                "hunter_jacket", "riding_breeches", "soft_boots", "wide_brim_hat", "fencing_gloves"
            );
            case "NEW_PLATE" -> List.of(
                "brigandine", "mail_greaves", "light_sabatons", "open_helm", "mail_gauntlets"
            );
            case "NEW_ARCANE" -> List.of(
                "phantom_vestment", "phantom_chausses", "phantom_boots", "phantom_crown", "phantom_gloves"
            );
            default -> throw new IllegalArgumentException("Unknown armor family: " + family);
        };
    }

    static Build hand(
        String code,
        List<String> hands,
        String armorFamily,
        Position position,
        TargetingTactic tactic
    ) {
        final var items = new ArrayList<>(hands);
        items.addAll(armor(armorFamily));
        return custom(code, items, position, tactic, Map.of());
    }

    static Build custom(
        String code,
        List<String> itemCodes,
        Position position,
        TargetingTactic tactic,
        Map<String, Assignment> assignments
    ) {
        return new Build(code, itemCodes, position, tactic, assignments);
    }

    private static Build fromItems(String code, List<Item> items, Position position, TargetingTactic tactic) {
        final var assignments = new LinkedHashMap<String, Assignment>();
        for (final var item : items) {
            item.modifier().ifPresent(modifier -> assignments.put(
                item.object().code(),
                new Assignment(modifier.activeEnum(), item.rarity())
            ));
        }
        return custom(code, items.stream().map(item -> item.object().code()).toList(), position, tactic, assignments);
    }

    private static ItemObject object(String code) {
        final var result = OBJECTS.get(code);
        if (result == null) {
            throw new IllegalArgumentException("SCALING_V3 has no obtainable item: " + code);
        }
        return result;
    }

    private static Modifier modifier(ActiveEnum skill) {
        final var result = MODIFIERS.get(skill);
        if (result == null) {
            throw new IllegalArgumentException("SCALING_V3 has no modifier for: " + skill);
        }
        return result;
    }

    private static void validate(List<String> itemCodes, Map<String, Assignment> assignments) {
        final var occupied = EnumSet.noneOf(PersonageSlot.class);
        for (final var code : itemCodes) {
            final var item = object(code);
            for (final var slot : item.slots()) {
                if (!occupied.add(slot)) {
                    throw new IllegalArgumentException("Overlapping equipment slot: " + slot);
                }
            }
            final var assignment = assignments.get(code);
            if (assignment != null) {
                final var itemModifier = modifier(assignment.skill());
                if (item.slots().stream().anyMatch(
                    slot -> !ModifierCompatibility.isCompatible(item, itemModifier, slot)
                )) {
                    throw new IllegalArgumentException("Incompatible modifier on " + code + ": " + assignment.skill());
                }
            }
        }
        if (!occupied.equals(EnumSet.allOf(PersonageSlot.class))) {
            throw new IllegalArgumentException("Representative build must occupy all seven slots: " + occupied);
        }
        if (!itemCodes.containsAll(assignments.keySet())) {
            throw new IllegalArgumentException("Modifier carrier must be equipped");
        }
    }

    record Assignment(ActiveEnum skill, ItemRarity rarity) {
        Assignment {
            Objects.requireNonNull(skill);
            Objects.requireNonNull(rarity);
            if (rarity == ItemRarity.COMMON) {
                throw new IllegalArgumentException("A modifier requires at least uncommon rarity");
            }
        }
    }

    record ArmorPair(String oldCode, String newCode, String family) {
    }

    record Build(
        String code,
        List<String> itemCodes,
        Position position,
        TargetingTactic tactic,
        Map<String, Assignment> assignments
    ) {
        Build {
            Objects.requireNonNull(code);
            Objects.requireNonNull(position);
            Objects.requireNonNull(tactic);
            itemCodes = List.copyOf(itemCodes);
            assignments = Map.copyOf(assignments);
            validate(itemCodes, assignments);
        }

        List<Item> items(int level) {
            if (level < 0) {
                throw new IllegalArgumentException("Enhance level must not be negative");
            }
            return itemCodes.stream().map(itemCode -> {
                final var assignment = assignments.get(itemCode);
                return new Item(
                    object(itemCode),
                    assignment == null ? Optional.empty() : Optional.of(modifier(assignment.skill())),
                    assignment == null ? ItemRarity.COMMON : assignment.rarity(),
                    level
                );
            }).toList();
        }

        BattlePersonage personage(int level) {
            final var result = BattlePersonage.forScalingSkillsV2Combat(
                items(level),
                position,
                Map.of(),
                Optional.of(code)
            );
            result.setTargetingTactic(tactic);
            return result;
        }

        Build replaceItem(String oldCode, String newCode) {
            if (!itemCodes.contains(oldCode)) {
                throw new IllegalArgumentException("Cannot replace unequipped item: " + oldCode);
            }
            if (!object(oldCode).slots().equals(object(newCode).slots())) {
                throw new IllegalArgumentException("Replacement must occupy the same slots");
            }
            final var updated = new LinkedHashMap<>(assignments);
            final var assignment = updated.remove(oldCode);
            if (assignment != null) {
                updated.put(newCode, assignment);
            }
            return new Build(
                code,
                itemCodes.stream().map(itemCode -> itemCode.equals(oldCode) ? newCode : itemCode).toList(),
                position,
                tactic,
                updated
            );
        }

        Build withArmor(String family) {
            var result = this;
            for (final var newCode : armor(family)) {
                final var replacementSlots = object(newCode).slots();
                final var oldCode = result.itemCodes().stream()
                    .filter(itemCode -> object(itemCode).slots().equals(replacementSlots))
                    .findFirst().orElseThrow();
                result = result.replaceItem(oldCode, newCode);
            }
            return result;
        }

        Build withAssignments(Map<String, Assignment> updated) {
            return new Build(code, itemCodes, position, tactic, updated);
        }

        Build renamed(String updated) {
            return new Build(updated, itemCodes, position, tactic, assignments);
        }

        Build withPosition(Position updated) {
            return new Build(code, itemCodes, updated, tactic, assignments);
        }
    }
}
