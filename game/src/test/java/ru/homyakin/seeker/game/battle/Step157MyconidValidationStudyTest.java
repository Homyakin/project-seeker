package ru.homyakin.seeker.game.battle;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step157MyconidValidationStudy.Sample;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.EquipmentPowerCalculator;

/** Matrix-only checks: no Battle.process, NPC generation, application, database or bot startup. */
class Step157MyconidValidationStudyTest {
    private static final List<Sample> CASES = Step157MyconidValidationStudy.cases();

    @Test
    void fixedMatrixHasTheDeclaredCountsAndStableIds() {
        Assertions.assertEquals(359, CASES.size());
        Assertions.assertEquals(8620, Step157MyconidValidationStudy.battleCount(CASES));
        Assertions.assertEquals(Map.of("A_MAIN", 105L, "B_SPECIALIZED", 36L, "C_COUNTERPLAY", 108L,
            "D_EFFECTS", 18L, "E_CAUSAL", 72L, "F_FULL_DEFAULTS", 20L), CASES.stream()
                .collect(Collectors.groupingBy(Sample::family, Collectors.counting())));
        for (int index = 0; index < CASES.size(); index++) {
            final var sample = CASES.get(index);
            Assertions.assertEquals(index + 1, sample.caseId());
            Assertions.assertEquals(sample.cell().size(), sample.members().size());
            Assertions.assertEquals(sample.family().equals("E_CAUSAL") ? 20 : 10, sample.pairs());
        }
    }

    @Test
    void seedGroupsAreSharedOnlyForDeclaredEffectsOrCausalComparisons() {
        final var bySeedGroup = CASES.stream().collect(Collectors.groupingBy(Sample::seedGroup));
        Assertions.assertEquals(281, bySeedGroup.size());
        for (final var group : bySeedGroup.values()) {
            if (group.size() == 6) {
                Assertions.assertTrue(group.stream().allMatch(sample -> sample.family().equals("E_CAUSAL")));
                Assertions.assertEquals(6, group.stream().map(Sample::variant).distinct().count());
            } else if (group.size() == 2) {
                Assertions.assertEquals(List.of("A_MAIN", "D_EFFECTS"), group.stream().map(Sample::family).toList());
                Assertions.assertEquals(group.getFirst().members(), group.getLast().members());
                Assertions.assertFalse(group.getFirst().cell().effects());
                Assertions.assertTrue(group.getLast().cell().effects());
            } else {
                Assertions.assertEquals(1, group.size());
            }
        }
        final var first = CASES.getFirst();
        Assertions.assertEquals(20_261_115_158_000L, Step157MyconidValidationStudy.generationSeed(first, 0));
        Assertions.assertEquals(30_261_115_158_000L, Step157MyconidValidationStudy.battleSeed(first, 0));
    }

    @Test
    void appendedParticipantsDoNotChangeExistingMembers() {
        for (final var group : causalGroups()) {
            final var variants = group.stream().collect(Collectors.toMap(Sample::variant, Function.identity()));
            final var base = variants.get("BASE").members();
            for (final var variant : List.of("NORMAL_APPEND", "WEAK_APPEND")) {
                final var changed = variants.get(variant).members();
                Assertions.assertEquals(base.size() + 1, changed.size());
                Assertions.assertEquals(base, changed.subList(0, base.size()));
            }
            Assertions.assertEquals(Step157MyconidValidationStudy.weakWithDefaults(),
                variants.get("WEAK_APPEND").members().getLast());
        }
    }

    @Test
    void unequippingArmorPreservesWeaponsOtherMembersAndValidSlots() {
        for (final var group : causalGroups()) {
            final var variants = group.stream().collect(Collectors.toMap(Sample::variant, Function.identity()));
            final var base = variants.get("BASE").members();
            final var changed = variants.get("UNEQUIP_ARMOR").members();
            Assertions.assertEquals(base.subList(1, base.size()), changed.subList(1, changed.size()));
            Assertions.assertEquals(weapons(base.getFirst().items()), weapons(changed.getFirst().items()));
            Assertions.assertEquals(base.getFirst().position(), changed.getFirst().position());
            Assertions.assertEquals(base.getFirst().tactic(), changed.getFirst().tactic());
            Assertions.assertTrue(EquipmentPowerCalculator.calculate(changed.getFirst().items()).unscaled()
                < EquipmentPowerCalculator.calculate(base.getFirst().items()).unscaled());
        }
        for (final var sample : CASES) {
            for (final var member : sample.members()) {
                final var occupied = EnumSet.noneOf(PersonageSlot.class);
                for (final var item : member.items()) {
                    for (final var slot : item.object().slots()) {
                        Assertions.assertTrue(occupied.add(slot));
                    }
                }
                Assertions.assertEquals(EnumSet.allOf(PersonageSlot.class), occupied);
                Assertions.assertTrue(EquipmentPowerCalculator.calculate(member.items()).unscaled() > 0);
            }
        }
    }

    @Test
    void rarityWithoutModifierIsExplicitlyANegativeControl() {
        int negativeControls = 0;
        for (final var group : causalGroups()) {
            final var variants = group.stream().collect(Collectors.toMap(Sample::variant, Function.identity()));
            final var base = variants.get("BASE").members();
            final var changed = variants.get("ONE_RARITY");
            if (changed.change().startsWith("NO_MODIFIER_NEGATIVE_CONTROL")) {
                ++negativeControls;
                Assertions.assertTrue(base.stream().flatMap(member -> member.items().stream())
                    .allMatch(item -> item.modifier().isEmpty()));
                for (int index = 0; index < base.size(); index++) {
                    Assertions.assertEquals(EquipmentPowerCalculator.calculate(base.get(index).items()),
                        EquipmentPowerCalculator.calculate(changed.members().get(index).items()));
                }
            }
        }
        Assertions.assertEquals(4, negativeControls);
    }

    private static List<List<Sample>> causalGroups() {
        return CASES.stream().filter(sample -> sample.family().equals("E_CAUSAL"))
            .collect(Collectors.groupingBy(Sample::seedGroup)).values().stream().toList();
    }

    private static List<Item> weapons(List<Item> items) {
        return items.stream().filter(item -> item.object().slots().stream()
            .anyMatch(slot -> slot == PersonageSlot.MAIN_HAND || slot == PersonageSlot.OFF_HAND)).toList();
    }
}
