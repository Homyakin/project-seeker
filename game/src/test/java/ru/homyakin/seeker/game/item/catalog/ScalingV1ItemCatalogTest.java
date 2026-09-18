package ru.homyakin.seeker.game.item.catalog;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.storm.ItemProgression;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.ReferencePowerCalculator;
import ru.homyakin.seeker.locale.Language;

class ScalingV1ItemCatalogTest {
    private static final Set<Integer> APPROVED_GROWTH_BASES = Set.of(
        60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 720
    );

    @Test
    void catalogMatchesEveryApprovedItemField() {
        final var objects = catalog().itemObjects();
        final var actual = objects.stream().collect(Collectors.toMap(ItemObject::code, ScalingV1ItemCatalogTest::fingerprint));

        Assertions.assertEquals(expectedFingerprints(EXPECTED_ITEMS), actual);
        Assertions.assertEquals(62, objects.size());
        Assertions.assertTrue(objects.stream().allMatch(it -> it.progressionVersion() == ItemProgressionVersion.V1));
        Assertions.assertTrue(objects.stream().allMatch(it -> it.locales().keySet().equals(Set.of(Language.RU))));
    }

    @Test
    void defaultItemsMatchEveryApprovedFieldAndStayOutsideDropCatalog() {
        final var catalog = catalog();
        final var actual = catalog.defaultItems().stream()
            .collect(Collectors.toMap(ItemObject::code, ScalingV1ItemCatalogTest::fingerprint));

        Assertions.assertEquals(expectedFingerprints(EXPECTED_DEFAULT_ITEMS), actual);
        Assertions.assertEquals(7, catalog.defaultItems().size());
        Assertions.assertTrue(catalog.defaultItems().stream()
            .allMatch(it -> it.progressionVersion() == ItemProgressionVersion.V1));
        Assertions.assertTrue(catalog.defaultItems().stream()
            .noneMatch(defaultItem -> catalog.itemObjects().stream().anyMatch(it -> it.code().equals(defaultItem.code()))));

        Assertions.assertEquals(840, catalog.defaultItems().stream().mapToInt(ItemObject::health).sum());
        Assertions.assertEquals(180, catalog.defaultItems().stream()
            .flatMap(it -> it.attacks().stream())
            .filter(it -> it.isAvailableAt(1))
            .mapToInt(ItemAttack::attack)
            .sum());
        Assertions.assertEquals(300, catalog.defaultItems().stream()
            .flatMap(it -> it.defense().stream())
            .mapToInt(it -> it.defense())
            .sum());
    }

    @Test
    void catalogHasApprovedSlotAndDefenseComposition() {
        final var objects = catalog().itemObjects();

        Assertions.assertAll(
            () -> Assertions.assertEquals(18, objects.stream().filter(it -> !it.attacks().isEmpty()).count()),
            () -> Assertions.assertEquals(44, objects.stream().filter(it -> it.defense().isPresent()).count()),
            () -> Assertions.assertTrue(objects.stream()
                .allMatch(it -> it.attacks().isEmpty() != it.defense().isEmpty())),
            () -> Assertions.assertEquals(5, countExactSlots(objects, PersonageSlot.MAIN_HAND)),
            () -> Assertions.assertEquals(9, countExactSlots(objects, PersonageSlot.OFF_HAND)),
            () -> Assertions.assertEquals(8, countExactSlots(
                objects,
                PersonageSlot.MAIN_HAND,
                PersonageSlot.OFF_HAND
            ))
        );

        for (final var slot : List.of(
            PersonageSlot.BODY,
            PersonageSlot.PANTS,
            PersonageSlot.SHOES,
            PersonageSlot.HELMET,
            PersonageSlot.GLOVES
        )) {
            Assertions.assertEquals(8, countExactSlots(objects, slot), slot.name());
        }
        for (final var type : DefenseType.values()) {
            Assertions.assertEquals(
                11,
                objects.stream().flatMap(it -> it.defense().stream()).filter(it -> it.defenseType() == type).count(),
                type.name()
            );
        }

        final var handConfigurations = countExactSlots(objects, PersonageSlot.MAIN_HAND)
            * countExactSlots(objects, PersonageSlot.OFF_HAND)
            + countExactSlots(objects, PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND);
        Assertions.assertEquals(53, handConfigurations);
        Assertions.assertEquals(1_736_704, handConfigurations * 8 * 8 * 8 * 8 * 8);
    }

    @Test
    void attackPartsUseApprovedRangesAndCorrectedTypes() {
        final var byCode = objectsByCode(catalog().itemObjects());

        Assertions.assertEquals(AttackType.PIERCE, byCode.get("dagger").attacks().getFirst().attackType());
        Assertions.assertEquals(AttackType.PIERCE, byCode.get("crossbow").attacks().getFirst().attackType());
        Assertions.assertEquals(
            List.of(
                new ItemAttack(AttackType.MAGICAL, 1, 2, 240),
                new ItemAttack(AttackType.MAGICAL, 2, 2, 120)
            ),
            byCode.get("staff").attacks()
        );
        Assertions.assertEquals(
            List.of(
                new ItemAttack(AttackType.PIERCE, 1, 3, 300),
                new ItemAttack(AttackType.PIERCE, 3, 3, 120)
            ),
            byCode.get("bow").attacks()
        );
        Assertions.assertEquals(
            List.of(
                new ItemAttack(AttackType.PIERCE, 1, 4, 360),
                new ItemAttack(AttackType.PIERCE, 3, 4, 120)
            ),
            byCode.get("longbow").attacks()
        );
        Assertions.assertEquals(
            List.of(
                new ItemAttack(AttackType.MAGICAL, 1, 4, 360),
                new ItemAttack(AttackType.MAGICAL, 3, 4, 120)
            ),
            byCode.get("battle_staff").attacks()
        );

        for (final var object : byCode.values()) {
            for (final var attack : object.attacks()) {
                Assertions.assertTrue(attack.minRange() >= 1, object.code());
                Assertions.assertTrue(attack.maxRange() >= attack.minRange(), object.code());
                Assertions.assertTrue(attack.maxRange() <= 4, object.code());
                Assertions.assertTrue(APPROVED_GROWTH_BASES.contains(attack.attack()), object.code());
            }
        }
    }

    @Test
    void everyGrowthChannelUsesApprovedBasesAndExactVersionOneProgression() {
        for (final var object : concat(catalog().itemObjects(), catalog().defaultItems())) {
            Assertions.assertTrue(object.health() == 0 || APPROVED_GROWTH_BASES.contains(object.health()), object.code());
            object.defense().ifPresent(defense ->
                Assertions.assertTrue(APPROVED_GROWTH_BASES.contains(defense.defense()), object.code())
            );
            object.attacks().forEach(attack ->
                Assertions.assertTrue(APPROVED_GROWTH_BASES.contains(attack.attack()), object.code())
            );

            for (var level = 0; level <= 21; level++) {
                final var state = ItemProgression.state(object, level);
                Assertions.assertEquals(expectedGrowth(object.health(), level), state.health(), object.code());
                for (var i = 0; i < object.attacks().size(); i++) {
                    Assertions.assertEquals(
                        expectedGrowth(object.attacks().get(i).attack(), level),
                        state.attacks().get(i).attack(),
                        object.code()
                    );
                }
                if (object.defense().isPresent()) {
                    Assertions.assertEquals(
                        expectedGrowth(object.defense().orElseThrow().defense(), level),
                        state.defense().orElseThrow().defense(),
                        object.code()
                    );
                }
            }
        }
    }

    @Test
    void approvedArmorFamiliesHaveExactTotals() {
        final var byCode = objectsByCode(catalog().itemObjects());

        assertFamily(byCode, "robe,cloth_chausses,cloth_boots,hood,cloth_gloves", "1380|CLOTH:360|4|10|0.09|132|0|5");
        assertFamily(byCode, "ceremonial_tunic,ceremonial_trousers,ceremonial_slippers,ceremonial_veil,ceremonial_gloves",
            "1380|CLOTH:360|9|5|0.22|126|0|10");
        assertFamily(byCode, "wizard_robe,arcane_chausses,arcane_boots,circlet,arcane_gloves",
            "1620|ARCANE:480|9|5|0.25|116|0|13");
        assertFamily(byCode, "phantom_vestment,phantom_chausses,phantom_boots,phantom_crown,phantom_gloves",
            "1620|ARCANE:480|2|10|0.06|126|0|5");
        assertFamily(byCode, "breastplate,leather_chausses,boots,leather_helm,leather_gloves",
            "1920|LEATHER:480|5|12|0.11|106|0|5");
        assertFamily(byCode, "hunter_jacket,riding_breeches,soft_boots,wide_brim_hat,fencing_gloves",
            "1920|LEATHER:480|5|7|0.21|112|0|10");
        assertFamily(byCode, "cuirass,greaves,sabatons,great_helm,gauntlets", "2280|PLATE:540|0|0|0|62|30|0");
        assertFamily(byCode, "brigandine,mail_greaves,light_sabatons,open_helm,mail_gauntlets",
            "1920|PLATE:480|4|3|0.09|84|12|5");
    }

    @Test
    void exactHandMirrorsRemainEqualAtEveryDistance() {
        final var byCode = objectsByCode(catalog().itemObjects());

        assertHandMirror(byCode, List.of("sword", "shortsword"), "two_handed_sword", true);
        assertHandMirror(byCode, List.of("mace", "club"), "sledgehammer", true);
        assertHandMirror(byCode, List.of("staff", "orb"), "halberd", false);
    }

    @Test
    void representativeFullEquipmentIsValidReferencePowerInput() {
        final var items = catalog().itemObjects();
        final var fullEquipment = List.of(
            Item.fromObject(findByExactSlots(items, PersonageSlot.MAIN_HAND)),
            Item.fromObject(findByExactSlots(items, PersonageSlot.OFF_HAND)),
            Item.fromObject(findByExactSlots(items, PersonageSlot.BODY)),
            Item.fromObject(findByExactSlots(items, PersonageSlot.PANTS)),
            Item.fromObject(findByExactSlots(items, PersonageSlot.SHOES)),
            Item.fromObject(findByExactSlots(items, PersonageSlot.HELMET)),
            Item.fromObject(findByExactSlots(items, PersonageSlot.GLOVES))
        );

        final var power = ReferencePowerCalculator.calculate(fullEquipment, 1.0);

        Assertions.assertTrue(power.survivability() > 0);
        Assertions.assertTrue(power.normalDamagePerTime() > 0);
        Assertions.assertEquals(power.unscaled(), power.displayed());
    }

    private static LoadedEquipmentCatalog catalog() {
        return EquipmentCatalogLoader.load(EquipmentCatalogVersion.SCALING_V1);
    }

    private static Map<String, ItemObject> objectsByCode(List<ItemObject> objects) {
        return objects.stream().collect(Collectors.toMap(ItemObject::code, Function.identity()));
    }

    private static long countExactSlots(List<ItemObject> objects, PersonageSlot... slots) {
        return objects.stream().filter(it -> it.slots().equals(Set.of(slots))).count();
    }

    private static ItemObject findByExactSlots(List<ItemObject> objects, PersonageSlot... slots) {
        final var expected = Set.of(slots);
        return objects.stream()
            .filter(it -> it.slots().equals(expected))
            .findFirst()
            .orElseThrow();
    }

    private static int expectedGrowth(int base, int level) {
        return base == 0 ? 0 : base + level * (base / 60);
    }

    private static List<ItemObject> concat(List<ItemObject> first, List<ItemObject> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }

    private static String fingerprint(ItemObject object) {
        final var slots = object.slots().stream()
            .sorted(java.util.Comparator.comparingInt(slot -> slot.id))
            .map(Enum::name)
            .collect(Collectors.joining("+"));
        final var attacks = object.attacks().isEmpty()
            ? "-"
            : object.attacks().stream()
                .map(it -> "%s:%d-%d=%d".formatted(it.attackType(), it.minRange(), it.maxRange(), it.attack()))
                .collect(Collectors.joining(","));
        final var defense = object.defense()
            .map(it -> "%s:%d".formatted(it.defenseType(), it.defense()))
            .orElse("-");
        final var locale = object.locales().get(Language.RU);
        return String.join(
            "|",
            object.code(),
            slots,
            Integer.toString(object.health()),
            attacks,
            defense,
            Integer.toString(object.critChance()),
            Integer.toString(object.dodgeChance()),
            decimal(object.critMultiplier()),
            Integer.toString(object.speed()),
            Integer.toString(object.baseThreat()),
            Integer.toString(object.impact()),
            locale.text(),
            locale.form().name()
        );
    }

    private static Map<String, String> expectedFingerprints(String rows) {
        return rows.strip().lines().collect(Collectors.toMap(
            line -> line.substring(0, line.indexOf('|')),
            Function.identity()
        ));
    }

    private static String decimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static void assertFamily(Map<String, ItemObject> byCode, String codes, String expected) {
        final var objects = Arrays.stream(codes.split(",")).map(byCode::get).toList();
        final var defenseType = objects.getFirst().defense().orElseThrow().defenseType();
        Assertions.assertTrue(objects.stream().allMatch(it -> it.defense().orElseThrow().defenseType() == defenseType));
        final var actual = String.join(
            "|",
            Integer.toString(objects.stream().mapToInt(ItemObject::health).sum()),
            "%s:%d".formatted(defenseType, objects.stream().mapToInt(it -> it.defense().orElseThrow().defense()).sum()),
            Integer.toString(objects.stream().mapToInt(ItemObject::critChance).sum()),
            Integer.toString(objects.stream().mapToInt(ItemObject::dodgeChance).sum()),
            decimalSum(objects),
            Integer.toString(objects.stream().mapToInt(ItemObject::speed).sum()),
            Integer.toString(objects.stream().mapToInt(ItemObject::baseThreat).sum()),
            Integer.toString(objects.stream().mapToInt(ItemObject::impact).sum())
        );
        Assertions.assertEquals(expected, actual, codes);
    }

    private static void assertHandMirror(
        Map<String, ItemObject> byCode,
        List<String> oneHandedCodes,
        String twoHandedCode,
        boolean sameType
    ) {
        final var oneHanded = oneHandedCodes.stream().map(byCode::get).toList();
        final var twoHanded = byCode.get(twoHandedCode);
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                oneHanded.stream().mapToInt(ItemObject::critChance).sum(),
                twoHanded.critChance()
            ),
            () -> Assertions.assertEquals(
                oneHanded.stream().mapToInt(ItemObject::dodgeChance).sum(),
                twoHanded.dodgeChance()
            ),
            () -> Assertions.assertEquals(
                decimalSum(oneHanded),
                decimal(twoHanded.critMultiplier())
            ),
            () -> Assertions.assertEquals(oneHanded.stream().mapToInt(ItemObject::speed).sum(), twoHanded.speed()),
            () -> Assertions.assertEquals(
                oneHanded.stream().mapToInt(ItemObject::baseThreat).sum(),
                twoHanded.baseThreat()
            ),
            () -> Assertions.assertEquals(oneHanded.stream().mapToInt(ItemObject::impact).sum(), twoHanded.impact())
        );
        for (var distance = 1; distance <= 4; distance++) {
            final var currentDistance = distance;
            final var pairAttack = oneHanded.stream()
                .flatMap(it -> it.attacks().stream())
                .filter(it -> it.isAvailableAt(currentDistance))
                .mapToInt(ItemAttack::attack)
                .sum();
            final var twoHandedAttack = twoHanded.attacks().stream()
                .filter(it -> it.isAvailableAt(currentDistance))
                .mapToInt(ItemAttack::attack)
                .sum();
            Assertions.assertEquals(pairAttack, twoHandedAttack, twoHandedCode + "@" + distance);
        }
        if (sameType) {
            Assertions.assertEquals(
                oneHanded.getFirst().attacks().getFirst().attackType(),
                twoHanded.attacks().getFirst().attackType()
            );
        }
    }

    private static String decimalSum(List<ItemObject> objects) {
        return objects.stream()
            .map(ItemObject::critMultiplier)
            .map(BigDecimal::valueOf)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .stripTrailingZeros()
            .toPlainString();
    }

    private static final String EXPECTED_ITEMS = """
        sword|MAIN_HAND|0|SLASH:1-1=360|-|2|1|0.05|18|8|0|Меч|MASCULINE
        rapier|MAIN_HAND|0|PIERCE:1-2=300|-|5|2|0.1|24|3|1|Рапира|FEMININE
        mace|MAIN_HAND|0|BLUNT:1-1=360|-|2|0|0.05|14|10|0|Булава|FEMININE
        spear|MAIN_HAND|0|PIERCE:1-2=240|-|3|1|0.05|28|5|3|Копьё|NEUTER
        staff|MAIN_HAND|0|MAGICAL:1-2=240,MAGICAL:2-2=120|-|3|1|0.08|16|2|4|Посох|MASCULINE
        shortsword|OFF_HAND|0|SLASH:1-1=120|-|1|1|0.03|12|3|0|Короткий Меч|MASCULINE
        club|OFF_HAND|0|BLUNT:1-1=120|-|1|0|0.02|10|8|0|Дубина|FEMININE
        dagger|OFF_HAND|0|PIERCE:1-1=120|-|5|1|0.1|14|1|1|Кинжал|MASCULINE
        dirk|OFF_HAND|0|PIERCE:1-1=120|-|2|4|0.05|24|2|3|Кортик|MASCULINE
        orb|OFF_HAND|0|MAGICAL:1-1=120|-|2|0|0.07|12|1|3|Сфера|FEMININE
        bow|MAIN_HAND+OFF_HAND|0|PIERCE:1-3=300,PIERCE:3-3=120|-|3|2|0.06|32|3|1|Лук|MASCULINE
        longbow|MAIN_HAND+OFF_HAND|0|PIERCE:1-4=360,PIERCE:3-4=120|-|4|1|0.1|24|4|2|Длинный Лук|MASCULINE
        crossbow|MAIN_HAND+OFF_HAND|0|PIERCE:1-3=420|-|5|1|0.15|28|3|7|Арбалет|MASCULINE
        two_handed_sword|MAIN_HAND+OFF_HAND|0|SLASH:1-1=480|-|3|2|0.08|30|11|0|Двуручный Меч|MASCULINE
        sledgehammer|MAIN_HAND+OFF_HAND|0|BLUNT:1-1=480|-|3|0|0.07|24|18|0|Кувалда|FEMININE
        halberd|MAIN_HAND+OFF_HAND|0|PIERCE:1-2=360|-|5|1|0.15|28|3|7|Алебарда|FEMININE
        battle_staff|MAIN_HAND+OFF_HAND|0|MAGICAL:1-4=360,MAGICAL:3-4=120|-|4|1|0.1|24|4|2|Боевой Посох|MASCULINE
        ritual_staff|MAIN_HAND+OFF_HAND|0|MAGICAL:1-3=420|-|5|1|0.15|28|3|7|Ритуальный Посох|MASCULINE
        buckler|OFF_HAND|240|-|CLOTH:60|0|3|0|20|2|1|Баклер|MASCULINE
        shield|OFF_HAND|360|-|LEATHER:120|0|2|0|16|4|1|Щит|MASCULINE
        tower_shield|OFF_HAND|420|-|PLATE:120|0|0|0|10|10|0|Башенный Щит|MASCULINE
        tome|OFF_HAND|300|-|ARCANE:120|2|0|0.05|18|0|3|Том|MASCULINE
        robe|BODY|480|-|CLOTH:120|1|2|0.03|36|0|1|Мантия|FEMININE
        cloth_chausses|PANTS|360|-|CLOTH:60|1|2|0.02|26|0|1|Тканевые Шоссы|PLURAL
        cloth_boots|SHOES|180|-|CLOTH:60|0|2|0|24|0|1|Тканевые Сапоги|PLURAL
        hood|HELMET|180|-|CLOTH:60|1|2|0.02|22|0|1|Капюшон|MASCULINE
        cloth_gloves|GLOVES|180|-|CLOTH:60|1|2|0.02|24|0|1|Перчатки|PLURAL
        wizard_robe|BODY|540|-|ARCANE:180|2|1|0.05|32|0|3|Магическая Мантия|FEMININE
        arcane_chausses|PANTS|360|-|ARCANE:120|1|1|0.04|26|0|3|Зачарованные Шоссы|PLURAL
        arcane_boots|SHOES|240|-|ARCANE:60|1|1|0.03|20|0|2|Магические Сапоги|PLURAL
        circlet|HELMET|240|-|ARCANE:60|4|1|0.1|18|0|3|Диадема|FEMININE
        arcane_gloves|GLOVES|240|-|ARCANE:60|1|1|0.03|20|0|2|Магические Перчатки|PLURAL
        breastplate|BODY|600|-|LEATHER:180|1|3|0.03|28|0|1|Нагрудник|MASCULINE
        leather_chausses|PANTS|420|-|LEATHER:120|1|2|0.02|22|0|1|Кожаные Шоссы|PLURAL
        boots|SHOES|300|-|LEATHER:60|1|3|0.02|20|0|1|Сапоги|PLURAL
        leather_helm|HELMET|300|-|LEATHER:60|1|2|0.02|18|0|1|Кожаный Шлем|MASCULINE
        leather_gloves|GLOVES|300|-|LEATHER:60|1|2|0.02|18|0|1|Кожаные Перчатки|PLURAL
        cuirass|BODY|720|-|PLATE:240|0|0|0|18|10|0|Кираса|FEMININE
        greaves|PANTS|480|-|PLATE:120|0|0|0|14|7|0|Латные Поножи|PLURAL
        sabatons|SHOES|360|-|PLATE:60|0|0|0|10|5|0|Латные Сапоги|PLURAL
        great_helm|HELMET|360|-|PLATE:60|0|0|0|10|5|0|Латный Шлем|MASCULINE
        gauntlets|GLOVES|360|-|PLATE:60|0|0|0|10|3|0|Латные Рукавицы|PLURAL
        ceremonial_tunic|BODY|480|-|CLOTH:120|2|1|0.05|34|0|2|Церемониальная Туника|FEMININE
        ceremonial_trousers|PANTS|360|-|CLOTH:60|1|1|0.04|24|0|2|Церемониальные Штаны|PLURAL
        ceremonial_slippers|SHOES|180|-|CLOTH:60|1|1|0.02|22|0|2|Церемониальные Туфли|PLURAL
        ceremonial_veil|HELMET|180|-|CLOTH:60|4|1|0.08|22|0|2|Церемониальная Вуаль|FEMININE
        ceremonial_gloves|GLOVES|180|-|CLOTH:60|1|1|0.03|24|0|2|Церемониальные Перчатки|PLURAL
        phantom_vestment|BODY|540|-|ARCANE:180|1|2|0.03|34|0|1|Призрачное Облачение|NEUTER
        phantom_chausses|PANTS|360|-|ARCANE:120|0|2|0.01|24|0|1|Призрачные Шоссы|PLURAL
        phantom_boots|SHOES|240|-|ARCANE:60|0|2|0|24|0|1|Призрачные Сапоги|PLURAL
        phantom_crown|HELMET|240|-|ARCANE:60|1|2|0.02|22|0|1|Призрачный Венец|MASCULINE
        phantom_gloves|GLOVES|240|-|ARCANE:60|0|2|0|22|0|1|Призрачные Перчатки|PLURAL
        hunter_jacket|BODY|600|-|LEATHER:180|2|2|0.05|28|0|2|Охотничья Куртка|FEMININE
        riding_breeches|PANTS|420|-|LEATHER:120|0|1|0.04|24|0|2|Походные Штаны|PLURAL
        soft_boots|SHOES|300|-|LEATHER:60|0|1|0.03|22|0|2|Мягкие Сапоги|PLURAL
        wide_brim_hat|HELMET|300|-|LEATHER:60|3|1|0.06|18|0|2|Широкополая Шляпа|FEMININE
        fencing_gloves|GLOVES|300|-|LEATHER:60|0|2|0.03|20|0|2|Фехтовальные Перчатки|PLURAL
        brigandine|BODY|600|-|PLATE:180|1|1|0.02|24|4|1|Бригантина|FEMININE
        mail_greaves|PANTS|420|-|PLATE:120|1|1|0.02|18|3|1|Кольчужные Поножи|PLURAL
        light_sabatons|SHOES|300|-|PLATE:60|0|1|0|14|2|1|Облегчённые Сабатоны|PLURAL
        open_helm|HELMET|300|-|PLATE:60|1|0|0.03|14|2|1|Открытый Шлем|MASCULINE
        mail_gauntlets|GLOVES|300|-|PLATE:60|1|0|0.02|14|1|1|Кольчужные Рукавицы|PLURAL
        """;

    private static final String EXPECTED_DEFAULT_ITEMS = """
        main-fists|MAIN_HAND|0|BLUNT:1-1=120|-|1|1|0.05|12|2|0|Кулак|MASCULINE
        off-fists|OFF_HAND|0|BLUNT:1-1=60|-|1|1|0.03|8|1|0|Кулак|MASCULINE
        torn_jacket|BODY|300|-|CLOTH:60|0|1|0|24|2|0|Рваная Кофта|FEMININE
        leaky_pants|PANTS|180|-|CLOTH:60|0|1|0|20|2|0|Дырявые Штаны|PLURAL
        footcloths|SHOES|120|-|CLOTH:60|0|1|0|16|0|0|Портянки|PLURAL
        panama|HELMET|120|-|CLOTH:60|0|1|0|16|0|0|Панамка|FEMININE
        bandages|GLOVES|120|-|CLOTH:60|0|1|0|16|0|0|Бинты|PLURAL
        """;
}
