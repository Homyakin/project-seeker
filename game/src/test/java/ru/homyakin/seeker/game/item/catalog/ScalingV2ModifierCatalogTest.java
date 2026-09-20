package ru.homyakin.seeker.game.item.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.WordForm;

class ScalingV2ModifierCatalogTest {
    private static final Set<String> NEW_ITEM_CODES = Set.of(
        "battle_staff",
        "ritual_staff",
        "command_staff",
        "ceremonial_tunic",
        "ceremonial_trousers",
        "ceremonial_slippers",
        "ceremonial_veil",
        "ceremonial_gloves",
        "phantom_vestment",
        "phantom_chausses",
        "phantom_boots",
        "phantom_crown",
        "phantom_gloves",
        "hunter_jacket",
        "riding_breeches",
        "soft_boots",
        "wide_brim_hat",
        "fencing_gloves",
        "brigandine",
        "mail_greaves",
        "light_sabatons",
        "open_helm",
        "mail_gauntlets"
    );
    private static final Set<String> NEW_WEAPON_CODES = Set.of("battle_staff", "ritual_staff", "command_staff");

    @Test
    void modifierCatalogMatchesEveryApprovedField() {
        final var modifiers = catalog().modifiers();
        final var actual = modifiers.stream().collect(Collectors.toMap(Modifier::code, ScalingV2ModifierCatalogTest::fingerprint));

        Assertions.assertEquals(expectedFingerprints(EXPECTED_MODIFIERS), actual);
        Assertions.assertEquals(15, modifiers.size());
        Assertions.assertTrue(modifiers.stream().allMatch(it -> it.locales().keySet().equals(Set.of(Language.RU))));
        Assertions.assertTrue(modifiers.stream().allMatch(it -> it.availableOnSlots().size() == 2));
    }

    @Test
    void commonCompatibilityRuleCreatesApprovedBasketsAndExactly272Pairs() {
        final var catalog = catalog();
        final var objects = catalog.itemObjects();
        final var modifiers = catalog.modifiers();
        final var pairs = compatiblePairs(objects, modifiers);

        Assertions.assertEquals(272, pairs.size());
        Assertions.assertEquals(196, pairs.stream().filter(it -> !NEW_ITEM_CODES.contains(it.itemCode())).count());
        Assertions.assertEquals(76, pairs.stream().filter(it -> NEW_ITEM_CODES.contains(it.itemCode())).count());
        Assertions.assertEquals(24, pairs.stream().filter(it -> NEW_WEAPON_CODES.contains(it.itemCode())).count());
        Assertions.assertEquals(52, pairs.stream()
            .filter(it -> NEW_ITEM_CODES.contains(it.itemCode()))
            .filter(it -> !NEW_WEAPON_CODES.contains(it.itemCode()))
            .count());
        Assertions.assertEquals(220, pairs.stream()
            .filter(it -> !NEW_ITEM_CODES.contains(it.itemCode()) || NEW_WEAPON_CODES.contains(it.itemCode()))
            .count());

        final Map<String, Integer> expectedCountByModifier = Map.ofEntries(
            Map.entry("counter", 16),
            Map.entry("thorny", 16),
            Map.entry("double", 19),
            Map.entry("furious", 23),
            Map.entry("swift", 16),
            Map.entry("sharp", 19),
            Map.entry("knocking", 23),
            Map.entry("healing", 16),
            Map.entry("precise", 19),
            Map.entry("retreating", 16),
            Map.entry("cunning", 16),
            Map.entry("guarding", 12),
            Map.entry("penetrating", 19),
            Map.entry("charging", 19),
            Map.entry("disrupting", 23)
        );
        final var actualCountByModifier = pairs.stream().collect(Collectors.groupingBy(
            CompatiblePair::modifierCode,
            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
        ));
        Assertions.assertEquals(expectedCountByModifier, actualCountByModifier);

        assertBasket(objects, modifiers, PersonageSlot.MAIN_HAND, true, Set.of(
            ActiveEnum.DOUBLE_ATTACK,
            ActiveEnum.BERSERK,
            ActiveEnum.BLEEDING,
            ActiveEnum.KNOCKBACK,
            ActiveEnum.PRECISE_STRIKE,
            ActiveEnum.PENETRATION,
            ActiveEnum.ACCUMULATION,
            ActiveEnum.TEMPO_BREAK
        ));
        assertBasket(objects, modifiers, PersonageSlot.OFF_HAND, true, Set.of(
            ActiveEnum.DOUBLE_ATTACK,
            ActiveEnum.BERSERK,
            ActiveEnum.BLEEDING,
            ActiveEnum.KNOCKBACK,
            ActiveEnum.PRECISE_STRIKE,
            ActiveEnum.PENETRATION,
            ActiveEnum.ACCUMULATION,
            ActiveEnum.TEMPO_BREAK
        ));
        assertBasket(objects, modifiers, PersonageSlot.OFF_HAND, false, Set.of(
            ActiveEnum.BERSERK,
            ActiveEnum.KNOCKBACK,
            ActiveEnum.GUARD,
            ActiveEnum.TEMPO_BREAK
        ));
        assertBasket(objects, modifiers, PersonageSlot.BODY, false, Set.of(
            ActiveEnum.COUNTER_ATTACK,
            ActiveEnum.THORNS,
            ActiveEnum.SELF_HEAL,
            ActiveEnum.GUARD
        ));
        assertBasket(objects, modifiers, PersonageSlot.PANTS, false, Set.of(
            ActiveEnum.SELF_HEAL,
            ActiveEnum.RETREAT
        ));
        assertBasket(objects, modifiers, PersonageSlot.SHOES, false, Set.of(
            ActiveEnum.HIT_AND_RUN,
            ActiveEnum.RETREAT,
            ActiveEnum.FEINT
        ));
        assertBasket(objects, modifiers, PersonageSlot.HELMET, false, Set.of(
            ActiveEnum.COUNTER_ATTACK,
            ActiveEnum.HIT_AND_RUN
        ));
        assertBasket(objects, modifiers, PersonageSlot.GLOVES, false, Set.of(
            ActiveEnum.THORNS,
            ActiveEnum.FEINT
        ));
    }

    @Test
    void everyObjectAndSelectedSlotHasANonEmptyModifierBasket() {
        final var catalog = catalog();
        for (final var object : catalog.itemObjects()) {
            for (final var slot : object.slots()) {
                final var compatible = catalog.modifiers().stream()
                    .filter(modifier -> ModifierCompatibility.isCompatible(object, modifier, slot))
                    .toList();
                Assertions.assertFalse(compatible.isEmpty(), object.code() + "@" + slot);
            }
        }
    }

    @Test
    void everyTwoHandedObjectHasTheSameEightOptionsThroughEitherHand() {
        final var catalog = catalog();
        final var twoHanded = catalog.itemObjects().stream()
            .filter(it -> it.slots().equals(Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND)))
            .toList();
        Assertions.assertEquals(9, twoHanded.size());

        for (final var object : twoHanded) {
            final var mainHand = activeEnums(object, catalog.modifiers(), PersonageSlot.MAIN_HAND);
            final var offHand = activeEnums(object, catalog.modifiers(), PersonageSlot.OFF_HAND);
            Assertions.assertEquals(8, mainHand.size(), object.code());
            Assertions.assertEquals(mainHand, offHand, object.code());
        }
    }

    @Test
    void revisedLegacyMasksLeaveExactly60HistoricalAssignments() {
        final var productionObjects = loadProductionObjects();
        final var productionModifiers = loadProductionModifiers();
        final var revisedLegacyModifiers = catalog().modifiers().stream()
            .filter(modifier -> productionModifiers.stream().anyMatch(old -> old.code().equals(modifier.code())))
            .toList();
        final var oldPairs = compatiblePairs(productionObjects, productionModifiers);
        final var retainedPairs = compatiblePairs(productionObjects, revisedLegacyModifiers);
        final var historicalPairs = new java.util.HashSet<>(oldPairs);
        historicalPairs.removeAll(retainedPairs);

        Assertions.assertEquals(196, oldPairs.size());
        Assertions.assertEquals(136, retainedPairs.size());
        Assertions.assertEquals(60, historicalPairs.size());

        final Map<ActiveEnum, Long> expectedRemovedBySkill = new EnumMap<>(ActiveEnum.class);
        expectedRemovedBySkill.put(ActiveEnum.COUNTER_ATTACK, 8L);
        expectedRemovedBySkill.put(ActiveEnum.THORNS, 4L);
        expectedRemovedBySkill.put(ActiveEnum.BERSERK, 4L);
        expectedRemovedBySkill.put(ActiveEnum.HIT_AND_RUN, 4L);
        expectedRemovedBySkill.put(ActiveEnum.KNOCKBACK, 8L);
        expectedRemovedBySkill.put(ActiveEnum.SELF_HEAL, 8L);
        expectedRemovedBySkill.put(ActiveEnum.RETREAT, 4L);
        expectedRemovedBySkill.put(ActiveEnum.FEINT, 20L);

        final var newModifiersByCode = catalog().modifiers().stream()
            .collect(Collectors.toMap(Modifier::code, Modifier::activeEnum));
        final Map<ActiveEnum, Long> actualRemovedBySkill = historicalPairs.stream().collect(Collectors.groupingBy(
            pair -> newModifiersByCode.get(pair.modifierCode()),
            () -> new EnumMap<>(ActiveEnum.class),
            Collectors.counting()
        ));
        Assertions.assertEquals(expectedRemovedBySkill, actualRemovedBySkill);
    }

    private static LoadedEquipmentCatalog catalog() {
        return EquipmentCatalogLoader.load(EquipmentCatalogVersion.SCALING_V2);
    }

    private static Set<CompatiblePair> compatiblePairs(List<ItemObject> objects, List<Modifier> modifiers) {
        return objects.stream()
            .flatMap(object -> modifiers.stream()
                .filter(modifier -> object.slots().stream()
                    .anyMatch(slot -> ModifierCompatibility.isCompatible(object, modifier, slot)))
                .map(modifier -> new CompatiblePair(object.code(), modifier.code())))
            .collect(Collectors.toSet());
    }

    private static void assertBasket(
        List<ItemObject> objects,
        List<Modifier> modifiers,
        PersonageSlot slot,
        boolean attacking,
        Set<ActiveEnum> expected
    ) {
        final var candidates = objects.stream()
            .filter(it -> it.slots().contains(slot))
            .filter(it -> attacking == !it.attacks().isEmpty())
            .toList();
        Assertions.assertFalse(candidates.isEmpty());
        for (final var candidate : candidates) {
            Assertions.assertEquals(expected, activeEnums(candidate, modifiers, slot), candidate.code());
        }
    }

    private static Set<ActiveEnum> activeEnums(ItemObject object, List<Modifier> modifiers, PersonageSlot slot) {
        return modifiers.stream()
            .filter(modifier -> ModifierCompatibility.isCompatible(object, modifier, slot))
            .map(Modifier::activeEnum)
            .collect(Collectors.toSet());
    }

    private static String fingerprint(Modifier modifier) {
        final var slots = modifier.availableOnSlots().stream()
            .sorted(java.util.Comparator.comparingInt(slot -> slot.id))
            .map(Enum::name)
            .collect(Collectors.joining("+"));
        final var forms = modifier.locales().get(Language.RU).form();
        return String.join(
            "|",
            modifier.code(),
            modifier.activeEnum().name(),
            modifier.type().name(),
            slots,
            forms.get(WordForm.MASCULINE),
            forms.get(WordForm.FEMININE),
            forms.get(WordForm.NEUTER),
            forms.get(WordForm.PLURAL)
        );
    }

    private static Map<String, String> expectedFingerprints(String rows) {
        return rows.strip().lines().collect(Collectors.toMap(
            line -> line.substring(0, line.indexOf('|')),
            Function.identity()
        ));
    }

    private static List<ItemObject> loadProductionObjects() {
        return loadResource("game-data/item_objects_catalog.toml", ItemObjectsToml::load).itemObjects();
    }

    private static List<Modifier> loadProductionModifiers() {
        return loadResource("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load).modifiers();
    }

    private static <T> T loadResource(String path, Function<InputStream, T> loader) {
        final var stream = ScalingV2ModifierCatalogTest.class.getClassLoader().getResourceAsStream(path);
        Assertions.assertNotNull(stream, path);
        try (stream) {
            return loader.apply(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private record CompatiblePair(String itemCode, String modifierCode) {
    }

    private static final String EXPECTED_MODIFIERS = """
        counter|COUNTER_ATTACK|DEFENSE|BODY+HELMET|Ответный|Ответная|Ответное|Ответные
        thorny|THORNS|DEFENSE|BODY+GLOVES|Колючий|Колючая|Колючее|Колючие
        double|DOUBLE_ATTACK|ATTACK|MAIN_HAND+OFF_HAND|Двойной|Двойная|Двойное|Двойные
        furious|BERSERK|ANY|MAIN_HAND+OFF_HAND|Гневный|Гневная|Гневное|Гневные
        swift|HIT_AND_RUN|DEFENSE|SHOES+HELMET|Резвый|Резвая|Резвое|Резвые
        sharp|BLEEDING|ATTACK|MAIN_HAND+OFF_HAND|Острый|Острая|Острое|Острые
        knocking|KNOCKBACK|ANY|MAIN_HAND+OFF_HAND|Сбивающий|Сбивающая|Сбивающее|Сбивающие
        healing|SELF_HEAL|DEFENSE|BODY+PANTS|Лечебный|Лечебная|Лечебное|Лечебные
        precise|PRECISE_STRIKE|ATTACK|MAIN_HAND+OFF_HAND|Точный|Точная|Точное|Точные
        retreating|RETREAT|DEFENSE|PANTS+SHOES|Отступающий|Отступающая|Отступающее|Отступающие
        cunning|FEINT|ANY|SHOES+GLOVES|Хитрый|Хитрая|Хитрое|Хитрые
        guarding|GUARD|DEFENSE|OFF_HAND+BODY|Прикрывающий|Прикрывающая|Прикрывающее|Прикрывающие
        penetrating|PENETRATION|ATTACK|MAIN_HAND+OFF_HAND|Проникающий|Проникающая|Проникающее|Проникающие
        charging|ACCUMULATION|ATTACK|MAIN_HAND+OFF_HAND|Накопительный|Накопительная|Накопительное|Накопительные
        disrupting|TEMPO_BREAK|ANY|MAIN_HAND+OFF_HAND|Срывающий|Срывающая|Срывающее|Срывающие
        """;
}
