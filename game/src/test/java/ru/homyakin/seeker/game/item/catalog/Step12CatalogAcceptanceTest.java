package ru.homyakin.seeker.game.item.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.item.storm.ItemProgression;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.ReferencePowerCalculator;

/**
 * Pure catalog acceptance for step 12. It deliberately does not run statistical battles.
 */
class Step12CatalogAcceptanceTest {
    private static final LoadedEquipmentCatalog CATALOG = EquipmentCatalogLoader.loadValidated(
        EquipmentCatalogVersion.SCALING_V2
    );
    private static final Map<String, String> APPROVED_OBJECT_FINGERPRINTS = approvedObjectFingerprints("""
        arcane_boots=5c691f24cb35d525b29918ca05a7e0e9bf07bca0730129d18f2e33198dede7ca
        arcane_chausses=680042b71c33590a65f2424223c086c25f502add1b5413eeefbb335e3229d074
        arcane_gloves=3971772d54ac6a6b909bcbd32e170cbf6c614790b5c4928323737c4d8677d271
        bandages=fbe099c6bff90195644d6529c12085da291b94257b46b375a364aa4e29534ff3
        battle_staff=e8abcce702a3a0ba4a726e90a13fd3c9c67a6e524bc676307855b81f5dd63f4e
        boots=314fc037e34ff93819e00b9691c9ee7285e0c0b9671e3f2f83268fb577d69f00
        bow=5e7005f5a9a3a3ebc97b127c4c9dec9474d4cd2a78cc9528cb1655a02e5c662a
        breastplate=ee129f07090afb252fc742006b08078c3ce8afe3bd665e597b0a9353c500faa0
        brigandine=cf2e13b5b6bbb621aca9c57f43bf4ee28daf0b4a312e79f0b0239d9ffdd0359b
        buckler=e9ca0564a0bda2cf2efca68bdb2473cb46f1f02498694ca67124344667e53a50
        ceremonial_gloves=6bed9183611773db675aa5c943181b22e44cbce52f356824312af329b4861dde
        ceremonial_slippers=05e5baa3b85791550f84e44475968c1a6971e2fda8303844ce08b87fb5d173fd
        ceremonial_trousers=322dc9cb8b9ff8d7ab16104eacacb85f3a2957e331bc618d8aa9bae5c1ccb509
        ceremonial_tunic=2cfd7f020e20d199811b1392f09953e8416bcc43858b1982d7551a047f109213
        ceremonial_veil=3715ed7b40a901a2ccfadd2728a83570f046b04f9040e83e227dfe33f0f1cc45
        circlet=9ba67a807b849f9df6b01a0f829fa9ea1475b53f984ee16199b5440ee8796ad6
        cloth_boots=57f2618ff7383b7a795f78052f07b5540dc458ef1a4948c9b52078728d881e82
        cloth_chausses=72ed4828e44b7984f450bb69d861be63f6cc0b5d1f2d7cfdf5e74bd9d5d8c1b3
        cloth_gloves=7b0ce3f823da4f701abafa9e0ce157ab94d058a4dd938a1fb55899dceb2e7e33
        club=e590fe8531ba194f8c27c046b5df21e7c6186259a69d90f3ce91054fb491919b
        command_staff=473e1706ec3625e20bc558ac78a4998de14cd5f5b9e775aaa9537ebca79c77ce
        crossbow=0a1b46cd0e852f7f4c06d92ac00041670b0915d509a6b15979cf6bc715221c10
        cuirass=c3781a80641bab889bba9bd1935b464189436d374b7c91b4738380d0ffe16241
        dagger=388f01cdf80f067e5a46c3f9ebdf87a2482cbed65af26f8eb8b316e406f89bed
        dirk=5c925e3e44a7b3cf287d3a18eb0c693e4559d5f4ffd8b4c54879d91465c4a0c3
        fencing_gloves=c63b73dfde4efe122cc9152a7ff86ac02031d8e012260848d33a9bec62502cd0
        footcloths=57e2b492edda2d0d02def197c406957871fbcbc3425ff28edeacb9c28d2441a3
        gauntlets=1e76b6db6671ed8bcd2e722f98e84f18b270fe18e3f97c9803673316047d3f5c
        great_helm=ba1211bb8fae33e7e66b2f0d588fe458a3b08c7eb67da60dee1cb006ab99d20e
        greaves=410ad38bcf3ac9d99db490e86d4ffe6845cc96b23dfdaa9e1b9a531bac48a28c
        halberd=d035ea7d09e4a203fe7ea4b1963e9732083043fbf832b0427c4b7944c56d675d
        hood=1d4848674c418d4335aa199d6e2b54f711757e9b0c4a0569fcc256ec8424aa94
        hunter_jacket=6d4297adb2a7b17b144a43e0388649ec2756fb9c8990a7ba5a0757465d06edce
        leaky_pants=ef07472afc5472c1743e580bfba47b4ba4ae811f182c68197d1cd22e0b9a032a
        leather_chausses=436057f56aaf27c1c6091da58351a0ae7bebff94c7127f576f52840a713a2c4a
        leather_gloves=72267750ee908a500c1a284c185956262d1312d04e41219f242e68d2ea55e2a1
        leather_helm=1005d3314bf148c1c859d98b29f19323e032bb8c919bb49217ea911b569ceece
        light_sabatons=785a8973a9f3de1e8be6702e425646acf298b458e4803ddc8e1fb34db7c0581d
        longbow=7199d774b8f1f3ff2f266ad387e01c7a77b6ceba4ae58c9ea696c256a46b6c78
        mace=b747cb1d6ea940db7a5d7130aa7d1dc77858e5a6ee916808b1a02072000162ed
        mail_gauntlets=331229a8fd9304d37757f959e189702e1a18ab77b2f3166f32f15f7852d46acf
        mail_greaves=45ed214efb24049b156c9d1bcf285847144ddece97ab1a07fec0f48743ab5ddc
        main-fists=d894b2ed155de3d95f4e6db00b0ef6c11a7a5178d47dc557ec3d13d4b994dc3d
        off-fists=a961048e8556b6a4a6e28df27e1344642ba0b35d320675fedc5da5b1b1a2d657
        open_helm=d56ffeb0e3c5095ce7b234219a15604cdac8fb001b870cac88c2aa61a42fa952
        orb=dd858e9bbb80e8b8d99394897092b81164408cb0769ab9ec5ce4d4ee96c18780
        panama=f81b05e7bda92898eb7a44439bb5c1a9244fc159217fd565be2d447058f6f789
        phantom_boots=b15ad4e0dc68fd08260bcf90ca59386365dc1a15261562f419e40556dda7bb8f
        phantom_chausses=d1a92a0b8a997c67e5973638116d57acdffb1458ae05d7abc1d3fd2ff0cf7aa3
        phantom_crown=9a2ec5b0e4c448d6f1c2d50babc72c6a1d0b4724bdff5e85c80cf5c4da9ff3b2
        phantom_gloves=f2fa4f0f4c81cefe4661447875372d3b3267d16eef30ddcfddcf9c0acb414323
        phantom_vestment=dafe8c2ce3ab38685c282fce781ff1964e2e777bc5ab265d4fb4304b9ab21d96
        rapier=dd36583fcfbd4dc5e23d6e05c5e12b15c660dda459cf2b7d2b0f32b6b5475ce5
        riding_breeches=372be675c793148f75b4854efc91084f832e8f87d1893eb2523b40201668d26d
        ritual_staff=273e09b1cd310392bee2d8579f080d56a88fc62c5b7038186a9ff6864ecf8992
        robe=321b011f8b096a5ee7dc6940b9f54b53de43c8bc6fceaff382cbbd588897bd15
        sabatons=f6adb4377de8e2a97389979cfa185f8f713c3422d25a81bd871a89fdfb6ec78b
        shield=ea1479bf721d276a77170252f1191af1192ed3a0243010630238b9dde52fb87f
        shortsword=7d85f07a6023b898453837942bceaaf8ed0f0e475e9c25bef838059542971102
        sledgehammer=43993f3be7b3cf702ba23ff265f732dc5f00b6948c059c092d914fbd1599e903
        soft_boots=834ab59758ed6859346bed9373e30c89b3596f80c165cf2c990d167e55ca7e7f
        spear=275b6076c31e830fb82188926c79b1c4bb86e23040a5968e6d38df77c82740ef
        staff=40edb3fddb91ba394cdffd6976355cb6cb54fa6c0ad24aac05da233ad5ad1fee
        sword=797b5336da5675e716cfbfdada0b6641e74b46a8de7e96f6771ef049a457ae00
        tome=d61ea07d011d893d46e5370bfcd5f9e267dbfd85caca849029449b082c815e97
        torn_jacket=f270650b2298b40d98dca0e104f4154cd4277bb9944cd25f59407eb75a5b4ac0
        tower_shield=8c3940e75be5fb2d31403431c4504bbe204ac4b776903c800de3fc344a80c2fd
        two_handed_sword=e68ee2d380908f6a7e56afbdfa1eb86d2790e64cfe19ddf1e024ab5ec1aead4f
        wide_brim_hat=a668bc857120d4e5ce64407351d5afef5c2da538be8e386729a06b96a5a87f65
        wizard_robe=b0c2507866490cb64b4d75572daba6ce61d3528db7d46657e5f650b30b14e06b
        """);
    private static final Set<Integer> APPROVED_GROWTH_BASES = Set.of(
        60,
        120,
        180,
        240,
        300,
        360,
        420,
        480,
        540,
        600,
        720
    );
    private static final List<Integer> CONTROL_LEVELS = List.of(0, 3, 6, 10, 20, 21);
    private static final List<ArmorPair> ARMOR_PAIRS = List.of(
        new ArmorPair("robe", "ceremonial_tunic"),
        new ArmorPair("cloth_chausses", "ceremonial_trousers"),
        new ArmorPair("cloth_boots", "ceremonial_slippers"),
        new ArmorPair("hood", "ceremonial_veil"),
        new ArmorPair("cloth_gloves", "ceremonial_gloves"),
        new ArmorPair("wizard_robe", "phantom_vestment"),
        new ArmorPair("arcane_chausses", "phantom_chausses"),
        new ArmorPair("arcane_boots", "phantom_boots"),
        new ArmorPair("circlet", "phantom_crown"),
        new ArmorPair("arcane_gloves", "phantom_gloves"),
        new ArmorPair("breastplate", "hunter_jacket"),
        new ArmorPair("leather_chausses", "riding_breeches"),
        new ArmorPair("boots", "soft_boots"),
        new ArmorPair("leather_helm", "wide_brim_hat"),
        new ArmorPair("leather_gloves", "fencing_gloves"),
        new ArmorPair("cuirass", "brigandine"),
        new ArmorPair("greaves", "mail_greaves"),
        new ArmorPair("sabatons", "light_sabatons"),
        new ArmorPair("great_helm", "open_helm"),
        new ArmorPair("gauntlets", "mail_gauntlets")
    );
    private static final List<ArmorFamily> ARMOR_FAMILIES = List.of(
        armorFamily(
            "old-cloth",
            DefenseType.CLOTH,
            "robe,cloth_chausses,cloth_boots,hood,cloth_gloves",
            new FamilyTotals(1380, 360, 4, 10, 9, 132, 0, 5)
        ),
        armorFamily(
            "new-cloth",
            DefenseType.CLOTH,
            "ceremonial_tunic,ceremonial_trousers,ceremonial_slippers,ceremonial_veil,ceremonial_gloves",
            new FamilyTotals(1380, 360, 9, 5, 22, 126, 0, 10)
        ),
        armorFamily(
            "old-arcane",
            DefenseType.ARCANE,
            "wizard_robe,arcane_chausses,arcane_boots,circlet,arcane_gloves",
            new FamilyTotals(1620, 480, 9, 5, 25, 116, 0, 13)
        ),
        armorFamily(
            "new-arcane",
            DefenseType.ARCANE,
            "phantom_vestment,phantom_chausses,phantom_boots,phantom_crown,phantom_gloves",
            new FamilyTotals(1620, 480, 2, 10, 6, 126, 0, 5)
        ),
        armorFamily(
            "old-leather",
            DefenseType.LEATHER,
            "breastplate,leather_chausses,boots,leather_helm,leather_gloves",
            new FamilyTotals(1920, 480, 5, 12, 11, 106, 0, 5)
        ),
        armorFamily(
            "new-leather",
            DefenseType.LEATHER,
            "hunter_jacket,riding_breeches,soft_boots,wide_brim_hat,fencing_gloves",
            new FamilyTotals(1920, 480, 5, 7, 21, 112, 0, 10)
        ),
        armorFamily(
            "old-plate",
            DefenseType.PLATE,
            "cuirass,greaves,sabatons,great_helm,gauntlets",
            new FamilyTotals(2280, 540, 0, 0, 0, 62, 30, 0)
        ),
        armorFamily(
            "new-plate",
            DefenseType.PLATE,
            "brigandine,mail_greaves,light_sabatons,open_helm,mail_gauntlets",
            new FamilyTotals(1920, 480, 4, 3, 9, 84, 12, 5)
        )
    );

    @Test
    void releaseAndEveryProgressionStateMatchApprovedInvariants() {
        final var catalogObjects = CATALOG.itemObjects();
        final var defaultObjects = CATALOG.defaultItems();
        final var allObjects = new ArrayList<ItemObject>(catalogObjects.size() + defaultObjects.size());
        allObjects.addAll(catalogObjects);
        allObjects.addAll(defaultObjects);

        Assertions.assertEquals(63, catalogObjects.size());
        Assertions.assertEquals(7, defaultObjects.size());
        Assertions.assertEquals(70, allObjects.stream().map(ItemObject::code).distinct().count());
        Assertions.assertTrue(allObjects.stream().allMatch(it -> it.progressionVersion() == ItemProgressionVersion.V1));
        Assertions.assertTrue(catalogObjects.stream().noneMatch(defaultObjects::contains));

        for (final var object : allObjects) {
            assertApprovedGrowthChannels(object);
            assertEveryProgressionState(object);
            assertEveryProgressionTransition(object);
        }
    }

    @Test
    void everyObjectMatchesApprovedBaseStatFingerprint() {
        final var actualByCode = allObjectsByCode();

        Assertions.assertEquals(
            APPROVED_OBJECT_FINGERPRINTS.keySet(),
            actualByCode.keySet(),
            "Approved fingerprint must cover every catalog and default object"
        );
        for (final var code : APPROVED_OBJECT_FINGERPRINTS.keySet().stream().sorted().toList()) {
            final var canonicalObject = canonicalObject(actualByCode.get(code));
            Assertions.assertEquals(
                APPROVED_OBJECT_FINGERPRINTS.get(code),
                sha256(canonicalObject),
                () -> "Base stats changed for %s. Actual fingerprint input:%n%s".formatted(code, canonicalObject)
            );
        }
    }

    @Test
    void twentyArmorPairsAndEightFamilyTotalsMatchApprovedAlternatives() {
        final var byCode = objectsByCode();
        final var pairedNewCodes = new HashSet<String>();

        Assertions.assertEquals(20, ARMOR_PAIRS.size());
        for (final var pair : ARMOR_PAIRS) {
            final var oldObject = byCode.get(pair.oldCode());
            final var newObject = byCode.get(pair.newCode());
            Assertions.assertNotNull(oldObject, pair.oldCode());
            Assertions.assertNotNull(newObject, pair.newCode());
            pairedNewCodes.add(pair.newCode());

            Assertions.assertEquals(oldObject.slots(), newObject.slots(), pair.toString());
            Assertions.assertEquals(
                oldObject.defense().orElseThrow().defenseType(),
                newObject.defense().orElseThrow().defenseType(),
                pair.toString()
            );
            assertHorizontalAlternative(oldObject, newObject);
        }
        Assertions.assertEquals(20, pairedNewCodes.size());

        for (final var family : ARMOR_FAMILIES) {
            assertFamilyTotals(family, byCode);
        }
    }

    @Test
    void exhaustivePhysicalLoadoutsMatchApprovedExtremaAndCounts() {
        final var bySlots = CATALOG.itemObjects().stream().collect(java.util.stream.Collectors.groupingBy(
            ItemObject::slots
        ));
        final var mainHands = exactSlot(bySlots, PersonageSlot.MAIN_HAND);
        final var offHands = exactSlot(bySlots, PersonageSlot.OFF_HAND);
        final var twoHanded = exactSlots(bySlots, PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND);
        final var bodies = exactSlot(bySlots, PersonageSlot.BODY);
        final var pants = exactSlot(bySlots, PersonageSlot.PANTS);
        final var shoes = exactSlot(bySlots, PersonageSlot.SHOES);
        final var helmets = exactSlot(bySlots, PersonageSlot.HELMET);
        final var gloves = exactSlot(bySlots, PersonageSlot.GLOVES);
        final var handBlocks = handBlocks(mainHands, offHands, twoHanded);

        Assertions.assertAll(
            () -> Assertions.assertEquals(5, mainHands.size()),
            () -> Assertions.assertEquals(9, offHands.size()),
            () -> Assertions.assertEquals(9, twoHanded.size()),
            () -> Assertions.assertEquals(54, handBlocks.size()),
            () -> Assertions.assertEquals(8, bodies.size()),
            () -> Assertions.assertEquals(8, pants.size()),
            () -> Assertions.assertEquals(8, shoes.size()),
            () -> Assertions.assertEquals(8, helmets.size()),
            () -> Assertions.assertEquals(8, gloves.size())
        );

        final var extrema = new CatalogExtrema();
        long loadoutCount = 0;
        for (final var hands : handBlocks) {
            for (final var body : bodies) {
                for (final var pantsObject : pants) {
                    for (final var shoesObject : shoes) {
                        for (final var helmet : helmets) {
                            for (final var glovesObject : gloves) {
                                extrema.accept(List.of(
                                    hands,
                                    RawStats.from(body),
                                    RawStats.from(pantsObject),
                                    RawStats.from(shoesObject),
                                    RawStats.from(helmet),
                                    RawStats.from(glovesObject)
                                ));
                                loadoutCount++;
                            }
                        }
                    }
                }
            }
        }

        Assertions.assertEquals(1_769_472, loadoutCount);
        Assertions.assertAll(
            () -> assertExtremum(extrema.health, "health", 1380, 2_176, 2700, 5),
            () -> assertExtremum(extrema.attack, "attack", 240, 131_072, 480, 491_520),
            () -> assertExtremum(extrema.critChance, "crit chance", 2, 270, 19, 450),
            () -> assertExtremum(extrema.dodgeChance, "dodge chance", 0, 20, 18, 36),
            () -> assertExtremum(extrema.critMultiplierPoints, "crit multiplier points", 125, 72, 165, 54),
            () -> assertExtremum(extrema.speed, "speed", 86, 6, 184, 24),
            () -> assertExtremum(extrema.threat, "threat", 2, 7_776, 50, 1),
            () -> assertExtremum(extrema.impact, "impact", 80, 8, 100, 63)
        );
    }

    @Test
    void approvedHandRelationshipsRemainExactOrExposeTheirTradeoffAtEveryControlLevel() {
        final var byCode = objectsByCode();

        assertSwordPairTradeoff(byCode);
        assertHandMirror(byCode, List.of("mace", "club"), "sledgehammer", true);
        assertHandMirror(byCode, List.of("staff", "orb"), "halberd", false);
        assertCommandStaffTradeoff(byCode);
    }

    @Test
    void referencePowerDoesNotDependOnModifier() {
        final var byCode = objectsByCode();
        final var referenceObjects = List.of(
            byCode.get("sword"),
            byCode.get("shortsword"),
            byCode.get("breastplate"),
            byCode.get("leather_chausses"),
            byCode.get("boots"),
            byCode.get("leather_helm"),
            byCode.get("leather_gloves")
        );
        final var withoutModifier = referenceObjects.stream()
            .map(object -> new Item(object, Optional.empty(), ItemRarity.LEGENDARY, 20))
            .toList();
        final var expected = ReferencePowerCalculator.calculate(withoutModifier, 1.0);

        Assertions.assertTrue(expected.unscaled() > 0);
        for (final var modifier : CATALOG.modifiers()) {
            final var compatibleIndex = IntStream.range(0, referenceObjects.size())
                .filter(index -> referenceObjects.get(index).slots().stream().anyMatch(slot ->
                    ModifierCompatibility.isCompatible(referenceObjects.get(index), modifier, slot)
                ))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No reference object accepts " + modifier.code()));
            final var withModifier = new ArrayList<>(withoutModifier);
            final var object = referenceObjects.get(compatibleIndex);
            withModifier.set(
                compatibleIndex,
                new Item(object, Optional.of(modifier), ItemRarity.LEGENDARY, 20)
            );

            Assertions.assertEquals(
                expected,
                ReferencePowerCalculator.calculate(withModifier, 1.0),
                modifier.code()
            );
        }
    }

    private static void assertApprovedGrowthChannels(ItemObject object) {
        Assertions.assertTrue(
            object.health() == 0 || APPROVED_GROWTH_BASES.contains(object.health()),
            object.code() + "/health"
        );
        Assertions.assertFalse(object.attacks().isEmpty() && object.defense().isEmpty(), object.code());
        object.attacks().forEach(attack -> Assertions.assertTrue(
            APPROVED_GROWTH_BASES.contains(attack.attack()),
            object.code() + "/attack"
        ));
        object.defense().ifPresent(defense -> Assertions.assertTrue(
            APPROVED_GROWTH_BASES.contains(defense.defense()),
            object.code() + "/defense"
        ));
    }

    private static void assertEveryProgressionState(ItemObject object) {
        for (var level = 0; level <= 21; level++) {
            final var currentLevel = level;
            final var state = ItemProgression.state(object, currentLevel);
            Assertions.assertEquals(
                expectedValue(object.health(), currentLevel),
                state.health(),
                stateLabel(object, currentLevel)
            );
            Assertions.assertEquals(object.attacks().size(), state.attacks().size(), stateLabel(object, currentLevel));
            for (var index = 0; index < object.attacks().size(); index++) {
                final var base = object.attacks().get(index);
                final var actual = state.attacks().get(index);
                Assertions.assertEquals(base.attackType(), actual.attackType(), stateLabel(object, currentLevel));
                Assertions.assertEquals(base.minRange(), actual.minRange(), stateLabel(object, currentLevel));
                Assertions.assertEquals(base.maxRange(), actual.maxRange(), stateLabel(object, currentLevel));
                Assertions.assertEquals(
                    expectedValue(base.attack(), currentLevel),
                    actual.attack(),
                    stateLabel(object, currentLevel)
                );
            }
            Assertions.assertEquals(
                object.defense().map(defense -> expectedValue(defense.defense(), currentLevel)),
                state.defense().map(defense -> defense.defense()),
                stateLabel(object, currentLevel)
            );

            final var item = new Item(object, Optional.empty(), ItemRarity.COMMON, currentLevel);
            Assertions.assertEquals(object.critChance(), item.critChance(), stateLabel(object, currentLevel));
            Assertions.assertEquals(object.dodgeChance(), item.dodgeChance(), stateLabel(object, currentLevel));
            Assertions.assertEquals(object.critMultiplier(), item.critMultiplier(), stateLabel(object, currentLevel));
            Assertions.assertEquals(object.speed(), item.speed(), stateLabel(object, currentLevel));
            Assertions.assertEquals(object.baseThreat(), item.baseThreat(), stateLabel(object, currentLevel));
            Assertions.assertEquals(object.impact(), item.impact(), stateLabel(object, currentLevel));
        }
    }

    private static void assertEveryProgressionTransition(ItemObject object) {
        for (var level = 0; level <= 20; level++) {
            final var transition = ItemProgression.transition(object, level);
            Assertions.assertEquals(expectedIncrease(object.health()), transition.health(), stateLabel(object, level));
            Assertions.assertEquals(object.attacks().size(), transition.attacks().size(), stateLabel(object, level));
            for (var index = 0; index < object.attacks().size(); index++) {
                final var base = object.attacks().get(index);
                final var actual = transition.attacks().get(index);
                Assertions.assertEquals(base.attackType(), actual.attackType(), stateLabel(object, level));
                Assertions.assertEquals(base.minRange(), actual.minRange(), stateLabel(object, level));
                Assertions.assertEquals(base.maxRange(), actual.maxRange(), stateLabel(object, level));
                Assertions.assertEquals(expectedIncrease(base.attack()), actual.attack(), stateLabel(object, level));
            }
            Assertions.assertEquals(
                object.defense().map(defense -> expectedIncrease(defense.defense())),
                transition.defense().map(defense -> defense.defense()),
                stateLabel(object, level)
            );
        }
    }

    private static int expectedValue(int base, int level) {
        return base == 0 ? 0 : base + level * (base / 60);
    }

    private static int expectedIncrease(int base) {
        return base == 0 ? 0 : base / 60;
    }

    private static String stateLabel(ItemObject object, int level) {
        return object.code() + "/+" + level;
    }

    private static void assertHorizontalAlternative(ItemObject oldObject, ItemObject newObject) {
        final var oldValues = comparisonValues(oldObject);
        final var newValues = comparisonValues(newObject);
        var oldWins = false;
        var newWins = false;
        for (var index = 0; index < oldValues.length; index++) {
            oldWins |= oldValues[index] > newValues[index];
            newWins |= newValues[index] > oldValues[index];
        }
        Assertions.assertTrue(oldWins, oldObject.code() + " has no strict advantage over " + newObject.code());
        Assertions.assertTrue(newWins, newObject.code() + " has no strict advantage over " + oldObject.code());

        final var defenseType = oldObject.defense().orElseThrow().defenseType();
        if (defenseType != DefenseType.PLATE) {
            Assertions.assertEquals(oldObject.health(), newObject.health(), oldObject.code());
            Assertions.assertEquals(
                oldObject.defense().orElseThrow().defense(),
                newObject.defense().orElseThrow().defense(),
                oldObject.code()
            );
        } else {
            Assertions.assertTrue(oldObject.health() > newObject.health(), oldObject.code());
            Assertions.assertTrue(
                oldObject.defense().orElseThrow().defense() >= newObject.defense().orElseThrow().defense(),
                oldObject.code()
            );
        }
    }

    private static double[] comparisonValues(ItemObject object) {
        return new double[]{
            object.health(),
            object.defense().orElseThrow().defense(),
            object.critChance(),
            object.dodgeChance(),
            object.critMultiplier(),
            object.speed(),
            object.baseThreat(),
            object.impact()
        };
    }

    private static void assertFamilyTotals(ArmorFamily family, Map<String, ItemObject> byCode) {
        final var objects = family.codes().stream().map(byCode::get).toList();
        Assertions.assertEquals(5, objects.size(), family.name());
        Assertions.assertTrue(objects.stream().allMatch(java.util.Objects::nonNull), family.name());
        Assertions.assertTrue(objects.stream().allMatch(object ->
            object.defense().orElseThrow().defenseType() == family.defenseType()
        ), family.name());

        final var actual = new FamilyTotals(
            sum(objects, ItemObject::health),
            objects.stream().mapToInt(object -> object.defense().orElseThrow().defense()).sum(),
            sum(objects, ItemObject::critChance),
            sum(objects, ItemObject::dodgeChance),
            sum(objects, Step12CatalogAcceptanceTest::critMultiplierPoints),
            sum(objects, ItemObject::speed),
            sum(objects, ItemObject::baseThreat),
            sum(objects, ItemObject::impact)
        );
        Assertions.assertEquals(family.expected(), actual, family.name());
    }

    private static int sum(List<ItemObject> objects, ToIntFunction<ItemObject> field) {
        return objects.stream().mapToInt(field).sum();
    }

    private static List<RawStats> handBlocks(
        List<ItemObject> mainHands,
        List<ItemObject> offHands,
        List<ItemObject> twoHanded
    ) {
        final var result = new ArrayList<RawStats>();
        for (final var mainHand : mainHands) {
            for (final var offHand : offHands) {
                result.add(RawStats.from(List.of(mainHand, offHand)));
            }
        }
        twoHanded.forEach(object -> result.add(RawStats.from(List.of(object))));
        return List.copyOf(result);
    }

    private static List<ItemObject> exactSlot(
        Map<Set<PersonageSlot>, List<ItemObject>> bySlots,
        PersonageSlot slot
    ) {
        return bySlots.getOrDefault(Set.of(slot), List.of());
    }

    private static List<ItemObject> exactSlots(
        Map<Set<PersonageSlot>, List<ItemObject>> bySlots,
        PersonageSlot first,
        PersonageSlot second
    ) {
        return bySlots.getOrDefault(Set.of(first, second), List.of());
    }

    private static void assertExtremum(
        Extremum actual,
        String name,
        int minimum,
        long minimumCount,
        int maximum,
        long maximumCount
    ) {
        Assertions.assertAll(
            name,
            () -> Assertions.assertEquals(minimum, actual.minimum),
            () -> Assertions.assertEquals(minimumCount, actual.minimumCount),
            () -> Assertions.assertEquals(maximum, actual.maximum),
            () -> Assertions.assertEquals(maximumCount, actual.maximumCount)
        );
    }

    private static void assertHandMirror(
        Map<String, ItemObject> byCode,
        List<String> oneHandedCodes,
        String twoHandedCode,
        boolean sameAttackType
    ) {
        final var oneHandedObjects = oneHandedCodes.stream().map(byCode::get).toList();
        final var twoHandedObject = byCode.get(twoHandedCode);

        for (final var level : CONTROL_LEVELS) {
            final var oneHandedItems = oneHandedObjects.stream()
                .map(object -> new Item(object, Optional.empty(), ItemRarity.COMMON, level))
                .toList();
            final var twoHandedItems = List.of(
                new Item(twoHandedObject, Optional.empty(), ItemRarity.COMMON, level)
            );
            final var label = String.join("+", oneHandedCodes) + "<->" + twoHandedCode + "/+" + level;

            Assertions.assertAll(
                label,
                () -> Assertions.assertEquals(sumItems(oneHandedItems, Item::health), sumItems(twoHandedItems, Item::health)),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::critChance),
                    sumItems(twoHandedItems, Item::critChance)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::dodgeChance),
                    sumItems(twoHandedItems, Item::dodgeChance)
                ),
                () -> Assertions.assertEquals(
                    sumItemCritMultiplierPoints(oneHandedItems),
                    sumItemCritMultiplierPoints(twoHandedItems)
                ),
                () -> Assertions.assertEquals(sumItems(oneHandedItems, Item::speed), sumItems(twoHandedItems, Item::speed)),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::baseThreat),
                    sumItems(twoHandedItems, Item::baseThreat)
                ),
                () -> Assertions.assertEquals(sumItems(oneHandedItems, Item::impact), sumItems(twoHandedItems, Item::impact))
            );

            for (var distance = 1; distance <= 4; distance++) {
                final var oneHandedAttack = attackByType(oneHandedItems, distance);
                final var twoHandedAttack = attackByType(twoHandedItems, distance);
                if (sameAttackType) {
                    Assertions.assertEquals(oneHandedAttack, twoHandedAttack, label + "/distance=" + distance);
                } else {
                    Assertions.assertEquals(
                        oneHandedAttack.values().stream().mapToInt(Integer::intValue).sum(),
                        twoHandedAttack.values().stream().mapToInt(Integer::intValue).sum(),
                        label + "/distance=" + distance
                    );
                }
            }
        }
    }

    private static void assertSwordPairTradeoff(Map<String, ItemObject> byCode) {
        final var oneHandedObjects = List.of(byCode.get("sword"), byCode.get("shortsword"));
        final var twoHandedObject = byCode.get("two_handed_sword");

        for (final var level : CONTROL_LEVELS) {
            final var oneHandedItems = oneHandedObjects.stream()
                .map(object -> new Item(object, Optional.empty(), ItemRarity.COMMON, level))
                .toList();
            final var twoHandedItems = List.of(
                new Item(twoHandedObject, Optional.empty(), ItemRarity.COMMON, level)
            );
            final var label = "sword+shortsword<->two_handed_sword/+" + level;

            Assertions.assertAll(
                label,
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::health),
                    sumItems(twoHandedItems, Item::health)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::critChance),
                    sumItems(twoHandedItems, Item::critChance)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::dodgeChance),
                    sumItems(twoHandedItems, Item::dodgeChance)
                ),
                () -> Assertions.assertEquals(
                    sumItemCritMultiplierPoints(oneHandedItems),
                    sumItemCritMultiplierPoints(twoHandedItems)
                ),
                () -> Assertions.assertEquals(
                    sumItems(twoHandedItems, Item::speed) + 6,
                    sumItems(oneHandedItems, Item::speed)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::baseThreat),
                    sumItems(twoHandedItems, Item::baseThreat)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::impact),
                    sumItems(twoHandedItems, Item::impact)
                )
            );

            for (var distance = 1; distance <= 4; distance++) {
                final var oneHandedAttack = attackByType(oneHandedItems, distance);
                final var twoHandedAttack = attackByType(twoHandedItems, distance);
                final int pairTotal = oneHandedAttack.values().stream().mapToInt(Integer::intValue).sum();
                final int twoHandedTotal = twoHandedAttack.values().stream().mapToInt(Integer::intValue).sum();
                Assertions.assertEquals(
                    twoHandedTotal * 7,
                    pairTotal * 8,
                    label + "/distance=" + distance
                );
                Assertions.assertEquals(oneHandedAttack.keySet(), twoHandedAttack.keySet(), label);
            }
        }
    }

    private static void assertCommandStaffTradeoff(Map<String, ItemObject> byCode) {
        final var oneHandedObjects = List.of(byCode.get("staff"), byCode.get("orb"));
        final var twoHandedObject = byCode.get("command_staff");

        for (final var level : CONTROL_LEVELS) {
            final var oneHandedItems = oneHandedObjects.stream()
                .map(object -> new Item(object, Optional.empty(), ItemRarity.COMMON, level))
                .toList();
            final var twoHandedItems = List.of(
                new Item(twoHandedObject, Optional.empty(), ItemRarity.COMMON, level)
            );
            final var label = "staff+orb<->command_staff/+" + level;

            Assertions.assertAll(
                label,
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::health),
                    sumItems(twoHandedItems, Item::health)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::critChance),
                    sumItems(twoHandedItems, Item::critChance)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::dodgeChance),
                    sumItems(twoHandedItems, Item::dodgeChance)
                ),
                () -> Assertions.assertEquals(
                    sumItemCritMultiplierPoints(oneHandedItems),
                    sumItemCritMultiplierPoints(twoHandedItems)
                ),
                () -> Assertions.assertEquals(
                    sumItems(twoHandedItems, Item::speed) + 4,
                    sumItems(oneHandedItems, Item::speed)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::baseThreat),
                    sumItems(twoHandedItems, Item::baseThreat)
                ),
                () -> Assertions.assertEquals(
                    sumItems(oneHandedItems, Item::impact),
                    sumItems(twoHandedItems, Item::impact)
                )
            );

            for (var distance = 1; distance <= 2; distance++) {
                final int currentDistance = distance;
                final int pairAttack = attackByType(oneHandedItems, currentDistance).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
                final int commandStaffAttack = attackByType(twoHandedItems, currentDistance).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
                Assertions.assertAll(
                    label + "/distance=" + currentDistance,
                    () -> Assertions.assertTrue(commandStaffAttack > pairAttack),
                    () -> Assertions.assertEquals(commandStaffAttack * 3, pairAttack * 4)
                );
            }
        }
    }

    private static int sumItems(List<Item> items, ToIntFunction<Item> field) {
        return items.stream().mapToInt(field).sum();
    }

    private static int sumItemCritMultiplierPoints(List<Item> items) {
        return items.stream().mapToInt(item -> decimalPoints(item.critMultiplier())).sum();
    }

    private static Map<AttackType, Integer> attackByType(List<Item> items, int distance) {
        final Map<AttackType, Integer> result = new EnumMap<>(AttackType.class);
        items.stream()
            .flatMap(item -> item.itemAttacks().stream())
            .filter(attack -> attack.isAvailableAt(distance))
            .forEach(attack -> result.merge(attack.attackType(), attack.attack(), Math::addExact));
        return Map.copyOf(result);
    }

    private static Map<String, ItemObject> objectsByCode() {
        final var result = new HashMap<String, ItemObject>();
        for (final var object : CATALOG.itemObjects()) {
            final var previous = result.put(object.code(), object);
            if (previous != null) {
                throw new AssertionError("Duplicate item object code: " + object.code());
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, ItemObject> allObjectsByCode() {
        final var result = new HashMap<String, ItemObject>();
        final var allObjects = new ArrayList<ItemObject>(CATALOG.itemObjects());
        allObjects.addAll(CATALOG.defaultItems());
        for (final var object : allObjects) {
            final var previous = result.put(object.code(), object);
            if (previous != null) {
                throw new AssertionError("Duplicate item object code: " + object.code());
            }
        }
        return Map.copyOf(result);
    }

    private static String canonicalObject(ItemObject object) {
        final var slots = object.slots().stream()
            .map(Enum::name)
            .sorted()
            .collect(java.util.stream.Collectors.joining(","));
        final var attacks = object.attacks().stream()
            .map(attack -> "%s:%d@%d..%d".formatted(
                attack.attackType().name(),
                attack.attack(),
                attack.minRange(),
                attack.maxRange()
            ))
            .collect(java.util.stream.Collectors.joining(","));
        final var defense = object.defense()
            .map(value -> "%s:%d".formatted(value.defenseType().name(), value.defense()))
            .orElse("-");
        return String.join(
            "|",
            object.code(),
            "slots=" + slots,
            "health=" + object.health(),
            "attacks=" + (attacks.isEmpty() ? "-" : attacks),
            "defense=" + defense,
            "crit=" + object.critChance(),
            "dodge=" + object.dodgeChance(),
            "critMultiplier=" + Double.toString(object.critMultiplier()),
            "speed=" + object.speed(),
            "threat=" + object.baseThreat(),
            "impact=" + object.impact(),
            "progression=" + object.progressionVersion().name()
        );
    }

    private static Map<String, String> approvedObjectFingerprints(String source) {
        final var result = new HashMap<String, String>();
        source.lines().filter(line -> !line.isBlank()).forEach(line -> {
            final var parts = line.strip().split("=", 2);
            if (parts.length != 2 || parts[1].length() != 64) {
                throw new IllegalArgumentException("Invalid approved object fingerprint: " + line);
            }
            final var previous = result.put(parts[0], parts[1]);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate approved object fingerprint: " + parts[0]);
            }
        });
        return Map.copyOf(result);
    }

    private static String sha256(String value) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256").digest(
                value.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static int critMultiplierPoints(ItemObject object) {
        return decimalPoints(object.critMultiplier());
    }

    private static int decimalPoints(double value) {
        final var points = (int) Math.round(value * 100);
        Assertions.assertEquals(points / 100.0, value, 1e-9);
        return points;
    }

    private static ArmorFamily armorFamily(
        String name,
        DefenseType defenseType,
        String commaSeparatedCodes,
        FamilyTotals expected
    ) {
        return new ArmorFamily(name, defenseType, List.of(commaSeparatedCodes.split(",")), expected);
    }

    private record ArmorPair(String oldCode, String newCode) {
    }

    private record ArmorFamily(
        String name,
        DefenseType defenseType,
        List<String> codes,
        FamilyTotals expected
    ) {
    }

    private record FamilyTotals(
        int health,
        int defense,
        int critChance,
        int dodgeChance,
        int critMultiplierPoints,
        int speed,
        int threat,
        int impact
    ) {
    }

    private record RawStats(
        int health,
        int attack,
        int critChance,
        int dodgeChance,
        int critMultiplierPoints,
        int speed,
        int threat,
        int impact
    ) {
        private static RawStats from(ItemObject object) {
            return from(List.of(object));
        }

        private static RawStats from(List<ItemObject> objects) {
            return new RawStats(
                sum(objects, ItemObject::health),
                bestAttack(objects),
                sum(objects, ItemObject::critChance),
                sum(objects, ItemObject::dodgeChance),
                sum(objects, Step12CatalogAcceptanceTest::critMultiplierPoints),
                sum(objects, ItemObject::speed),
                sum(objects, ItemObject::baseThreat),
                sum(objects, ItemObject::impact)
            );
        }

        private static int bestAttack(List<ItemObject> objects) {
            return IntStream.rangeClosed(1, 4)
                .map(distance -> objects.stream()
                    .flatMap(object -> object.attacks().stream())
                    .filter(attack -> attack.isAvailableAt(distance))
                    .mapToInt(ItemAttack::attack)
                    .sum())
                .max()
                .orElseThrow();
        }
    }

    private static final class CatalogExtrema {
        private final Extremum health = new Extremum();
        private final Extremum attack = new Extremum();
        private final Extremum critChance = new Extremum();
        private final Extremum dodgeChance = new Extremum();
        private final Extremum critMultiplierPoints = new Extremum();
        private final Extremum speed = new Extremum();
        private final Extremum threat = new Extremum();
        private final Extremum impact = new Extremum();

        private void accept(List<RawStats> parts) {
            health.accept(parts.stream().mapToInt(RawStats::health).sum());
            attack.accept(parts.stream().mapToInt(RawStats::attack).sum());
            critChance.accept(parts.stream().mapToInt(RawStats::critChance).sum());
            dodgeChance.accept(parts.stream().mapToInt(RawStats::dodgeChance).sum());
            critMultiplierPoints.accept(
                decimalPoints(CombatRules.BASE_CRIT_MULTIPLIER)
                    + parts.stream().mapToInt(RawStats::critMultiplierPoints).sum()
            );
            speed.accept(parts.stream().mapToInt(RawStats::speed).sum());
            threat.accept(parts.stream().mapToInt(RawStats::threat).sum());
            impact.accept(Math.min(100, 80 + parts.stream().mapToInt(RawStats::impact).sum()));
        }
    }

    private static final class Extremum {
        private int minimum = Integer.MAX_VALUE;
        private long minimumCount;
        private int maximum = Integer.MIN_VALUE;
        private long maximumCount;

        private void accept(int value) {
            if (value < minimum) {
                minimum = value;
                minimumCount = 1;
            } else if (value == minimum) {
                minimumCount++;
            }
            if (value > maximum) {
                maximum = value;
                maximumCount = 1;
            } else if (value == maximum) {
                maximumCount++;
            }
        }
    }
}
