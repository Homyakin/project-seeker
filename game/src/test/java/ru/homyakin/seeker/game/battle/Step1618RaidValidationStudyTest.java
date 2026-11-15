package ru.homyakin.seeker.game.battle;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step1618RaidValidationStudy.Sample;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.EquipmentPowerCalculator;

/** Matrix and canonicalization only: no generation, Battle.process, database or application startup. */
class Step1618RaidValidationStudyTest {
    private static final List<Sample> CASES = Step1618RaidValidationStudy.cases();

    @Test
    void fixedMatrixHasTheDeclaredPerRaidAndPerFamilyCounts() {
        Assertions.assertEquals(1044, CASES.size());
        Assertions.assertEquals(18480, Step1618RaidValidationStudy.battleCount(CASES));
        Assertions.assertEquals(Map.of("WOLFPACK", 332L, "ZOMBIE_HORDE", 356L, "MAGGEESE_FLOCK", 356L),
            CASES.stream().collect(Collectors.groupingBy(sample -> sample.cell().encounter(), Collectors.counting())));
        Assertions.assertEquals(Map.of("A_MAIN", 336L, "B_TYPES", 324L, "D_EFFECTS", 36L,
            "E_CAUSAL", 216L, "F_DEFAULTS", 60L, "G_WORKABLE", 72L), CASES.stream()
                .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
        for (final var raid : List.of(RaidType.WOLFPACK, RaidType.ZOMBIE_HORDE, RaidType.MAGGEESE_FLOCK)) {
            final var selected = CASES.stream().filter(sample -> sample.cell().encounter().equals(raid.name())).toList();
            Assertions.assertEquals(raid == RaidType.WOLFPACK ? 5840 : 6320,
                Step1618RaidValidationStudy.battleCount(selected));
        }
        for (int index = 0; index < CASES.size(); index++) {
            final var sample = CASES.get(index);
            Assertions.assertEquals(index + 1, sample.caseId());
            Assertions.assertEquals(sample.cell().size(), sample.members().size());
            Assertions.assertEquals(switch (sample.family()) {
                case "E_CAUSAL" -> 5;
                case "F_DEFAULTS" -> 8;
                default -> 10;
            }, sample.pairs());
        }
    }

    @Test
    void seedGroupsAreSharedOnlyByDeclaredComparisonsAndAreFresh() {
        final var groups = CASES.stream().collect(Collectors.groupingBy(Sample::seedGroup));
        Assertions.assertEquals(828, groups.size());
        Assertions.assertEquals(Map.of(1, 756L, 2, 36L, 6, 36L), groups.values().stream()
            .collect(Collectors.groupingBy(List::size, Collectors.counting())));
        for (final var group : groups.values()) {
            Assertions.assertEquals(1, group.stream().map(sample -> sample.cell().encounter()).distinct().count());
            if (group.size() == 6) {
                Assertions.assertTrue(group.stream().allMatch(sample -> sample.family().equals("E_CAUSAL")));
                Assertions.assertEquals(6, group.stream().map(Sample::variant).distinct().count());
            } else if (group.size() == 2) {
                Assertions.assertEquals(List.of("A_MAIN", "D_EFFECTS"), group.stream().map(Sample::family).toList());
                Assertions.assertEquals(group.getFirst().members(), group.getLast().members());
                Assertions.assertFalse(group.getFirst().cell().effects());
                Assertions.assertTrue(group.getLast().cell().effects());
                final var before = group.getFirst().members().stream()
                    .map(member -> EquipmentPowerCalculator.calculate(member.items())).toList();
                final var after = group.getLast().members().stream()
                    .map(member -> EquipmentPowerCalculator.calculate(member.items())).toList();
                Assertions.assertEquals(before, after, "Effects must not change the permanent-power input");
                Assertions.assertEquals(group.getFirst().cell().raidLevel(), group.getLast().cell().raidLevel());
            }
        }
        Assertions.assertEquals(20_261_118_001_000L, Step1618RaidValidationStudy.generationSeed(CASES.getFirst(), 0));
        Assertions.assertEquals(30_261_118_001_000L, Step1618RaidValidationStudy.battleSeed(CASES.getFirst(), 0));
        for (final var sample : CASES) {
            Assertions.assertEquals(1, Step1618RaidValidationStudy.generationSeed(sample, 1)
                - Step1618RaidValidationStudy.generationSeed(sample, 0));
        }
    }

    @Test
    void replaySelectionIsFixedAtThirtyFourConditionsAndSixtyEightBattles() {
        final var ids = Step1618RaidValidationStudy.replayCaseIds(CASES);
        Assertions.assertEquals(34, ids.size());
        Assertions.assertEquals(34, ids.stream().distinct().count());
        final var selected = ids.stream().map(id -> CASES.get(id - 1)).toList();
        Assertions.assertEquals(Map.of("WOLFPACK", 10L, "ZOMBIE_HORDE", 12L, "MAGGEESE_FLOCK", 12L), selected.stream()
            .collect(Collectors.groupingBy(sample -> sample.cell().encounter(), Collectors.counting())));
        for (final var sample : selected) {
            if (sample.family().equals("A_MAIN")) {
                Assertions.assertEquals(Step123ReferenceTeams.Tier.MID, sample.cell().tier());
                Assertions.assertEquals(25, sample.cell().raidLevel());
            }
        }
    }

    @Test
    void typedControlsHaveTheDeclaredPureAttackAndArmorTypesWithoutInventedItemRoles() {
        for (final var sample : CASES.stream().filter(sample -> sample.family().equals("B_TYPES")).toList()) {
            final var attack = sample.cell().style().startsWith("SLASH") ? AttackType.SLASH : AttackType.PIERCE;
            final var defense = sample.cell().style().endsWith("LEATHER") ? DefenseType.LEATHER : DefenseType.CLOTH;
            for (final var member : sample.members()) {
                Assertions.assertEquals(List.of(attack), member.items().stream().flatMap(item -> item.itemAttacks().stream())
                    .map(value -> value.attackType()).distinct().toList());
                Assertions.assertEquals(List.of(defense), member.items().stream().flatMap(item -> item.itemDefense().stream())
                    .map(value -> value.defenseType()).distinct().toList());
                Assertions.assertTrue(member.items().stream().allMatch(item -> item.modifier().isEmpty()));
                Assertions.assertTrue(member.items().stream()
                    .allMatch(item -> item.enhanceLevel() == sample.cell().tier().level()));
            }
        }
    }

    @Test
    void everyMemberHasSevenLegalOccupiedSlotsAndFinitePositivePermanentPower() {
        for (final var sample : CASES) {
            for (final var member : sample.members()) {
                final var occupied = EnumSet.noneOf(PersonageSlot.class);
                for (final var item : member.items()) {
                    for (final var slot : item.object().slots()) {
                        Assertions.assertTrue(occupied.add(slot));
                    }
                }
                Assertions.assertEquals(EnumSet.allOf(PersonageSlot.class), occupied);
                final var power = EquipmentPowerCalculator.calculate(member.items()).unscaled();
                Assertions.assertTrue(Double.isFinite(power) && power > 0);
            }
        }
    }

    @Test
    void causalChangesPreserveOldMembersWeaponsAndExplicitNegativeControls() {
        int rarityNegativeControls = 0;
        for (final var group : CASES.stream().filter(sample -> sample.family().equals("E_CAUSAL"))
            .collect(Collectors.groupingBy(Sample::seedGroup)).values()) {
            final var variants = group.stream().collect(Collectors.toMap(Sample::variant, Function.identity()));
            final var base = variants.get("BASE").members();
            for (final var variant : List.of("NORMAL_APPEND", "WEAK_APPEND")) {
                final var changed = variants.get(variant).members();
                Assertions.assertEquals(base.size() + 1, changed.size());
                Assertions.assertEquals(base, changed.subList(0, base.size()));
            }
            Assertions.assertEquals(Step1618RaidValidationStudy.weakWithDefaults(),
                variants.get("WEAK_APPEND").members().getLast());
            for (final var variant : List.of("ONE_ENHANCE", "ONE_RARITY", "UNEQUIP_ARMOR")) {
                Assertions.assertEquals(base.size(), variants.get(variant).members().size());
            }
            final var unarmored = variants.get("UNEQUIP_ARMOR").members();
            Assertions.assertEquals(base.subList(1, base.size()), unarmored.subList(1, unarmored.size()));
            Assertions.assertEquals(weapons(base.getFirst().items()), weapons(unarmored.getFirst().items()));
            Assertions.assertEquals(base.getFirst().position(), unarmored.getFirst().position());
            Assertions.assertEquals(base.getFirst().tactic(), unarmored.getFirst().tactic());
            Assertions.assertTrue(EquipmentPowerCalculator.calculate(unarmored.getFirst().items()).unscaled()
                < EquipmentPowerCalculator.calculate(base.getFirst().items()).unscaled());
            final var rarity = variants.get("ONE_RARITY");
            if (rarity.change().startsWith("NO_MODIFIER_NEGATIVE_CONTROL")) {
                ++rarityNegativeControls;
                Assertions.assertTrue(base.stream().flatMap(member -> member.items().stream())
                    .allMatch(item -> item.modifier().isEmpty()));
                for (int index = 0; index < base.size(); index++) {
                    Assertions.assertEquals(EquipmentPowerCalculator.calculate(base.get(index).items()),
                        EquipmentPowerCalculator.calculate(rarity.members().get(index).items()));
                }
            }
        }
        Assertions.assertEquals(12, rarityNegativeControls);
    }

    @Test
    void canonicalizationChangesOnlyTheTwoActualSetArraysAndNeverImportantSequenceOrder() {
        final var object = new ItemObject("canonical_object", Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
            List.of(new ItemAttack(AttackType.SLASH, 1, 100), new ItemAttack(AttackType.PIERCE, 2, 150)),
            Optional.empty(), 100, 1, 1, 0.1, 100, 1, 0, null, Map.of());
        final var modifier = new Modifier("canonical_modifier",
            ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum.DOUBLE_ATTACK, ModifierType.ATTACK,
            Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND), Map.of());
        final com.fasterxml.jackson.databind.node.ObjectNode first = PveAdaptationStudy.JSON.valueToTree(
            Map.of("object", object, "modifier", modifier, "members", List.of("one", "two")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) first.get("object"))
            .putArray("slots").add("MAIN_HAND").add("OFF_HAND");
        ((com.fasterxml.jackson.databind.node.ObjectNode) first.get("modifier"))
            .putArray("availableOnSlots").add("MAIN_HAND").add("OFF_HAND");
        final var reorderedSets = first.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) reorderedSets.get("object"))
            .putArray("slots").add("OFF_HAND").add("MAIN_HAND");
        ((com.fasterxml.jackson.databind.node.ObjectNode) reorderedSets.get("modifier"))
            .putArray("availableOnSlots").add("OFF_HAND").add("MAIN_HAND");
        Assertions.assertEquals(Step1618RaidValidationStudy.canonicalTree(first),
            Step1618RaidValidationStudy.canonicalTree(reorderedSets));
        final var reorderedMembers = first.deepCopy();
        reorderedMembers.putArray("members").add("two").add("one");
        Assertions.assertNotEquals(Step1618RaidValidationStudy.canonicalTree(first),
            Step1618RaidValidationStudy.canonicalTree(reorderedMembers));
        final var reorderedAttacks = first.deepCopy();
        final var attacks = (com.fasterxml.jackson.databind.node.ArrayNode) reorderedAttacks.get("object").get("attacks");
        final var firstAttack = attacks.remove(0);
        attacks.add(firstAttack);
        Assertions.assertNotEquals(Step1618RaidValidationStudy.canonicalTree(first),
            Step1618RaidValidationStudy.canonicalTree(reorderedAttacks));
        Assertions.assertEquals("MAIN_HAND", first.get("object").get("slots").get(0).asText());

        final var unrelated = Map.of("slots", List.of("B", "A"), "availableOnSlots", List.of("Y", "X"));
        Assertions.assertEquals(PveAdaptationStudy.JSON.valueToTree(unrelated),
            Step1618RaidValidationStudy.canonicalTree(unrelated));
    }

    private static List<Item> weapons(List<Item> items) {
        return items.stream().filter(item -> item.object().slots().stream()
            .anyMatch(slot -> slot == PersonageSlot.MAIN_HAND || slot == PersonageSlot.OFF_HAND)).toList();
    }
}
