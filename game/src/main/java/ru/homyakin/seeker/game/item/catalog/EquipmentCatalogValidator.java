package ru.homyakin.seeker.game.item.catalog;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.item.storm.ItemProgression;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.WordForm;

/** Validates the immutable contract of an equipment catalog release. */
public final class EquipmentCatalogValidator {
    private static final int EXPECTED_MODIFIERS = 15;
    private static final int EXPECTED_DEFAULT_ITEMS = 7;
    private static final int MAX_ATTACK_RANGE = 4;
    private static final int MAX_VALIDATION_LEVEL = 21;
    private static final int GROWING_BASE_DIVISOR = 60;

    private static final Set<String> SCALING_V1_ITEM_CODES = Set.of(
        "sword", "rapier", "mace", "spear", "staff", "bow", "longbow", "crossbow",
        "two_handed_sword", "sledgehammer", "halberd", "shortsword", "club", "dagger", "dirk", "orb",
        "buckler", "shield", "tower_shield", "tome", "robe", "cuirass", "breastplate", "wizard_robe",
        "cloth_chausses", "leather_chausses", "greaves", "arcane_chausses", "cloth_boots", "boots",
        "sabatons", "arcane_boots", "hood", "leather_helm", "great_helm", "circlet", "cloth_gloves",
        "leather_gloves", "gauntlets", "arcane_gloves", "battle_staff", "ritual_staff", "ceremonial_tunic",
        "ceremonial_trousers", "ceremonial_slippers", "ceremonial_veil", "ceremonial_gloves",
        "phantom_vestment", "phantom_chausses", "phantom_boots", "phantom_crown", "phantom_gloves",
        "hunter_jacket", "riding_breeches", "soft_boots", "wide_brim_hat", "fencing_gloves", "brigandine",
        "mail_greaves", "light_sabatons", "open_helm", "mail_gauntlets"
    );
    private static final Set<String> SCALING_V2_ITEM_CODES = withAddedCode(
        SCALING_V1_ITEM_CODES,
        "command_staff"
    );
    private static final Map<String, PersonageSlot> DEFAULT_ITEM_SLOTS = Map.of(
        "main-fists", PersonageSlot.MAIN_HAND,
        "off-fists", PersonageSlot.OFF_HAND,
        "torn_jacket", PersonageSlot.BODY,
        "leaky_pants", PersonageSlot.PANTS,
        "footcloths", PersonageSlot.SHOES,
        "panama", PersonageSlot.HELMET,
        "bandages", PersonageSlot.GLOVES
    );
    private static final Map<String, ModifierSpecification> MODIFIER_SPECIFICATIONS = Map.ofEntries(
        modifier("counter", ActiveEnum.COUNTER_ATTACK, ModifierType.DEFENSE, PersonageSlot.BODY, PersonageSlot.HELMET),
        modifier("thorny", ActiveEnum.THORNS, ModifierType.DEFENSE, PersonageSlot.BODY, PersonageSlot.GLOVES),
        modifier("double", ActiveEnum.DOUBLE_ATTACK, ModifierType.ATTACK,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
        modifier("furious", ActiveEnum.BERSERK, ModifierType.ANY,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
        modifier("swift", ActiveEnum.HIT_AND_RUN, ModifierType.DEFENSE,
            PersonageSlot.SHOES, PersonageSlot.HELMET),
        modifier("sharp", ActiveEnum.BLEEDING, ModifierType.ATTACK,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
        modifier("knocking", ActiveEnum.KNOCKBACK, ModifierType.ANY,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
        modifier("healing", ActiveEnum.SELF_HEAL, ModifierType.DEFENSE,
            PersonageSlot.BODY, PersonageSlot.PANTS),
        modifier("precise", ActiveEnum.PRECISE_STRIKE, ModifierType.ATTACK,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
        modifier("retreating", ActiveEnum.RETREAT, ModifierType.DEFENSE,
            PersonageSlot.PANTS, PersonageSlot.SHOES),
        modifier("cunning", ActiveEnum.FEINT, ModifierType.ANY,
            PersonageSlot.SHOES, PersonageSlot.GLOVES),
        modifier("guarding", ActiveEnum.GUARD, ModifierType.DEFENSE,
            PersonageSlot.OFF_HAND, PersonageSlot.BODY),
        modifier("penetrating", ActiveEnum.PENETRATION, ModifierType.ATTACK,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
        modifier("charging", ActiveEnum.ACCUMULATION, ModifierType.ATTACK,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
        modifier("disrupting", ActiveEnum.TEMPO_BREAK, ModifierType.ANY,
            PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND)
    );
    private static final CatalogContract SCALING_V1_CONTRACT = new CatalogContract(
        "SCALING_V1",
        SkillFormulaVersion.SCALING_SKILLS_V1,
        SCALING_V1_ITEM_CODES,
        62,
        8,
        18,
        264
    );
    private static final CatalogContract SCALING_V2_CONTRACT = new CatalogContract(
        "SCALING_V2",
        SkillFormulaVersion.SCALING_SKILLS_V2,
        SCALING_V2_ITEM_CODES,
        63,
        9,
        19,
        272
    );

    private EquipmentCatalogValidator() {
    }

    public static void validate(LoadedEquipmentCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Loaded catalog must be specified");
        }
        switch (catalog.release().catalogVersion()) {
            case SCALING_V1 -> validate(catalog, SCALING_V1_CONTRACT);
            case SCALING_V2 -> validate(catalog, SCALING_V2_CONTRACT);
        }
    }

    private static void validate(LoadedEquipmentCatalog catalog, CatalogContract contract) {
        final var release = catalog.release();
        require(
            release.progressionVersion() == ItemProgressionVersion.V1,
            contract.name() + " must use item progression V1"
        );
        require(
            release.skillFormulaVersion() == contract.skillFormulaVersion(),
            "%s must use scaling skill formulas %s".formatted(
                contract.name(),
                contract.skillFormulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V1 ? "V1" : "V2"
            )
        );
        require(
            catalog.itemObjects().size() == contract.itemObjectCount(),
            "%s must contain %d item objects".formatted(contract.name(), contract.itemObjectCount())
        );
        require(catalog.modifiers().size() == EXPECTED_MODIFIERS,
            contract.name() + " must contain 15 modifiers");
        require(catalog.defaultItems().size() == EXPECTED_DEFAULT_ITEMS,
            contract.name() + " must contain 7 default items");

        validateUniqueCodes(catalog.itemObjects(), ItemObject::code, "item object");
        validateUniqueCodes(catalog.modifiers(), Modifier::code, "modifier");
        validateUniqueCodes(catalog.defaultItems(), ItemObject::code, "default item");
        final var allItemCodes = new HashSet<String>();
        catalog.itemObjects().forEach(item -> allItemCodes.add(item.code()));
        for (final var item : catalog.defaultItems()) {
            require(allItemCodes.add(item.code()), "Item and default item codes overlap: " + item.code());
        }

        require(codes(catalog.itemObjects(), ItemObject::code).equals(contract.itemCodes()),
            contract.name() + " item object codes differ from the approved release");
        require(codes(catalog.defaultItems(), ItemObject::code).equals(DEFAULT_ITEM_SLOTS.keySet()),
            contract.name() + " default item codes differ from the approved release");
        require(codes(catalog.modifiers(), Modifier::code).equals(MODIFIER_SPECIFICATIONS.keySet()),
            contract.name() + " modifier codes differ from the approved release");

        catalog.itemObjects().forEach(item -> validateItemObject(item, release));
        catalog.defaultItems().forEach(item -> validateItemObject(item, release));
        catalog.modifiers().forEach(EquipmentCatalogValidator::validateModifier);
        validateDefaultItemSlots(catalog.defaultItems());
        validateItemDistribution(catalog.itemObjects(), contract);
        validateModifierSpecifications(catalog.modifiers(), contract);
        validateCompatibility(catalog.itemObjects(), catalog.modifiers(), contract);
    }

    private static void validateItemObject(ItemObject item, EquipmentCatalogRelease release) {
        require(item.code() != null && !item.code().isBlank(), "Item object code must be specified");
        require(!item.slots().isEmpty(), "Item object must occupy a slot: " + item.code());
        require(item.progressionVersion() == release.progressionVersion(),
            "Unexpected progression version for item object: " + item.code());
        require(item.health() >= 0, "Health must be non-negative: " + item.code());
        validateGrowingBase(item.health(), "health", item.code());
        require(item.critChance() >= 0 && item.critChance() <= 100,
            "Critical chance must be in 0..100: " + item.code());
        require(item.dodgeChance() >= 0 && item.dodgeChance() < 100,
            "Dodge chance must be in 0..99: " + item.code());
        require(Double.isFinite(item.critMultiplier()) && item.critMultiplier() >= 0,
            "Critical multiplier bonus must be finite and non-negative: " + item.code());
        require(item.speed() >= 0, "Speed must be non-negative: " + item.code());
        require(item.baseThreat() >= 0, "Base threat must be non-negative: " + item.code());
        require(item.impact() >= 0, "Impact must be non-negative: " + item.code());
        require(!item.locales().isEmpty(), "Item object locales must be specified: " + item.code());
        item.validateLocale();
        item.locales().forEach((language, locale) -> {
            require(locale != null, "Missing item locale for " + item.code() + " and " + language);
            require(locale.text() != null && !locale.text().isBlank(), "Empty item text for " + item.code());
            require(locale.form() != null, "Missing item word form for " + item.code());
        });
        for (final var attack : item.attacks()) {
            require(attack.maxRange() <= MAX_ATTACK_RANGE, "Attack range exceeds four: " + item.code());
            validateGrowingBase(attack.attack(), "attack", item.code());
        }
        item.defense().ifPresent(defense -> {
            require(defense.defenseType() != null, "Defense type must be specified: " + item.code());
            validateGrowingBase(defense.defense(), "defense", item.code());
        });
        validateProgression(item);
    }

    private static void validateGrowingBase(int value, String field, String code) {
        require(value == 0 || value >= GROWING_BASE_DIVISOR,
            "%s must be zero or at least 60: %s".formatted(field, code));
        require(value % GROWING_BASE_DIVISOR == 0,
            "%s must be divisible by 60: %s".formatted(field, code));
    }

    private static void validateProgression(ItemObject item) {
        for (int level = 0; level <= MAX_VALIDATION_LEVEL; level++) {
            ItemProgression.state(item, level);
            if (level == MAX_VALIDATION_LEVEL) {
                continue;
            }
            final var delta = ItemProgression.transition(item, level);
            if (item.health() > 0) {
                require(delta.health() > 0, "Health must grow at every validated level: " + item.code());
            }
            require(delta.attacks().stream().allMatch(attack -> attack.attack() > 0),
                "Attack must grow at every validated level: " + item.code());
            require(delta.defense().map(defense -> defense.defense() > 0).orElse(true),
                "Defense must grow at every validated level: " + item.code());
        }
    }

    private static void validateModifier(Modifier modifier) {
        require(modifier.code() != null && !modifier.code().isBlank(), "Modifier code must be specified");
        require(modifier.activeEnum() != null, "Modifier skill must be specified: " + modifier.code());
        require(modifier.type() != null, "Modifier type must be specified: " + modifier.code());
        require(!modifier.availableOnSlots().isEmpty(), "Modifier slots must be specified: " + modifier.code());
        require(!modifier.locales().isEmpty(), "Modifier locales must be specified: " + modifier.code());
        modifier.validateLocale();
        modifier.locales().forEach((language, locale) -> {
            require(locale != null && locale.form() != null,
                "Missing modifier forms for " + modifier.code() + " and " + language);
            final var forms = locale.form();
            final var requiredForms = WordForm.languageRequiredForms(language);
            final boolean complete = requiredForms.map(forms.keySet()::containsAll).orElse(false);
            require(complete || forms.containsKey(WordForm.WITHOUT),
                "Incomplete modifier forms for " + modifier.code() + " and " + language);
            forms.forEach((form, text) -> require(text != null && !text.isBlank(),
                "Empty modifier form " + form + " for " + modifier.code()));
        });
    }

    private static void validateDefaultItemSlots(List<ItemObject> defaultItems) {
        for (final var item : defaultItems) {
            require(item.slots().equals(Set.of(DEFAULT_ITEM_SLOTS.get(item.code()))),
                "Unexpected slot for default item: " + item.code());
        }
    }

    private static void validateItemDistribution(List<ItemObject> items, CatalogContract contract) {
        require(countExactSlots(items, PersonageSlot.MAIN_HAND) == 5, "Expected 5 main-hand-only items");
        require(countExactSlots(items, PersonageSlot.OFF_HAND) == 9, "Expected 9 off-hand-only items");
        require(
            countExactSlots(items, PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND)
                == contract.twoHandedItemCount(),
            "Expected %d two-handed items".formatted(contract.twoHandedItemCount())
        );
        for (final var slot : List.of(
            PersonageSlot.BODY,
            PersonageSlot.PANTS,
            PersonageSlot.SHOES,
            PersonageSlot.HELMET,
            PersonageSlot.GLOVES
        )) {
            require(countExactSlots(items, slot) == 8, "Expected 8 item objects for slot " + slot);
            for (final var defenseType : DefenseType.values()) {
                final long typeCount = items.stream()
                    .filter(item -> item.slots().equals(Set.of(slot)))
                    .flatMap(item -> item.defense().stream())
                    .filter(defense -> defense.defenseType() == defenseType)
                    .count();
                require(typeCount == 2,
                    "Expected 2 item objects of defense type %s for slot %s".formatted(defenseType, slot));
            }
        }
        require(
            items.stream().filter(item -> !item.attacks().isEmpty()).count()
                == contract.attackingItemCount(),
            "Expected %d attacking item objects".formatted(contract.attackingItemCount())
        );
        require(items.stream().filter(item -> item.defense().isPresent()).count() == 44,
            "Expected 44 defensive item objects");
        for (final var defenseType : DefenseType.values()) {
            final long count = items.stream()
                .flatMap(item -> item.defense().stream())
                .filter(defense -> defense.defenseType() == defenseType)
                .count();
            require(count == 11, "Expected 11 defensive item objects of type " + defenseType);
        }
    }

    private static void validateModifierSpecifications(
        List<Modifier> modifiers,
        CatalogContract contract
    ) {
        final var skills = EnumSet.noneOf(ActiveEnum.class);
        for (final var modifier : modifiers) {
            final var expected = MODIFIER_SPECIFICATIONS.get(modifier.code());
            require(expected != null, "Unexpected modifier: " + modifier.code());
            require(modifier.activeEnum() == expected.activeEnum(), "Unexpected skill for modifier: " + modifier.code());
            require(modifier.type() == expected.type(), "Unexpected type for modifier: " + modifier.code());
            require(modifier.availableOnSlots().equals(expected.slots()),
                "Unexpected slots for modifier: " + modifier.code());
            require(skills.add(modifier.activeEnum()), "Skill has more than one modifier: " + modifier.activeEnum());
        }
        require(skills.equals(EnumSet.allOf(ActiveEnum.class)),
            contract.name() + " must contain every active skill");
    }

    private static void validateCompatibility(
        List<ItemObject> items,
        List<Modifier> modifiers,
        CatalogContract contract
    ) {
        long compatiblePairs = 0;
        for (final var item : items) {
            for (final var slot : item.slots()) {
                final var compatibleForSlot = compatibleModifierCodes(item, slot, modifiers);
                require(!compatibleForSlot.isEmpty(),
                    "No compatible modifier for item %s in slot %s".formatted(item.code(), slot));
            }
            if (item.slots().equals(Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND))) {
                require(
                    compatibleModifierCodes(item, PersonageSlot.MAIN_HAND, modifiers).equals(
                        compatibleModifierCodes(item, PersonageSlot.OFF_HAND, modifiers)
                    ),
                    "Two-handed item has different modifier baskets by selected slot: " + item.code()
                );
            }
            compatiblePairs += modifiers.stream()
                .filter(modifier -> item.slots().stream()
                    .anyMatch(slot -> ModifierCompatibility.isCompatible(item, modifier, slot)))
                .count();
        }
        require(compatiblePairs == contract.compatiblePairCount(),
            "Expected %d compatible object-modifier pairs, got %d".formatted(
                contract.compatiblePairCount(),
                compatiblePairs
            ));
    }

    private static Set<String> compatibleModifierCodes(
        ItemObject item,
        PersonageSlot slot,
        List<Modifier> modifiers
    ) {
        return modifiers.stream()
            .filter(modifier -> ModifierCompatibility.isCompatible(item, modifier, slot))
            .map(Modifier::code)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static long countExactSlots(List<ItemObject> items, PersonageSlot... slots) {
        final var expected = Set.of(slots);
        return items.stream().filter(item -> item.slots().equals(expected)).count();
    }

    private static <T> void validateUniqueCodes(List<T> values, Function<T, String> code, String type) {
        final var unique = new HashSet<String>();
        for (final var value : values) {
            require(unique.add(code.apply(value)), "Duplicate " + type + " code: " + code.apply(value));
        }
    }

    private static <T> Set<String> codes(List<T> values, Function<T, String> code) {
        return values.stream().map(code).collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> withAddedCode(Set<String> codes, String addedCode) {
        final var result = new HashSet<>(codes);
        require(result.add(addedCode), "Catalog code is already present: " + addedCode);
        return Set.copyOf(result);
    }

    private static Map.Entry<String, ModifierSpecification> modifier(
        String code,
        ActiveEnum activeEnum,
        ModifierType type,
        PersonageSlot... slots
    ) {
        return Map.entry(code, new ModifierSpecification(activeEnum, type, Set.of(slots)));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record ModifierSpecification(
        ActiveEnum activeEnum,
        ModifierType type,
        Set<PersonageSlot> slots
    ) {
    }

    private record CatalogContract(
        String name,
        SkillFormulaVersion skillFormulaVersion,
        Set<String> itemCodes,
        int itemObjectCount,
        int twoHandedItemCount,
        int attackingItemCount,
        int compatiblePairCount
    ) {
    }
}
