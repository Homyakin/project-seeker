package ru.homyakin.seeker.game.battle.skill;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public class ActiveSkillSlotsTest {
    @Test
    void loadsSlotsFromCatalogAndSortsByEnumName() {
        final var skills = ActiveSkillSlots.sortedSkills();
        Assertions.assertEquals(ActiveEnum.values().length, skills.size());
        Assertions.assertEquals(ActiveEnum.BERSERK, skills.getFirst());
        Assertions.assertEquals(ActiveEnum.THORNS, skills.getLast());
        Assertions.assertTrue(ActiveSkillSlots.slotsFor(ActiveEnum.DOUBLE_ATTACK).contains(PersonageSlot.MAIN_HAND));
        Assertions.assertFalse(ActiveSkillSlots.slotsFor(ActiveEnum.DOUBLE_ATTACK).contains(PersonageSlot.BODY));
    }

    @Test
    void filtersBySingleSlot() {
        final var mainHandSkills = ActiveSkillSlots.sortedSkills(Set.of(PersonageSlot.MAIN_HAND));
        Assertions.assertFalse(mainHandSkills.isEmpty());
        Assertions.assertTrue(mainHandSkills.stream().allMatch(
            skill -> ActiveSkillSlots.slotsFor(skill).contains(PersonageSlot.MAIN_HAND)
        ));
    }

    @Test
    void filtersByMultipleSlotsWithAnd() {
        final var filters = Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.BODY);
        final var skills = ActiveSkillSlots.sortedSkills(filters);
        Assertions.assertFalse(skills.isEmpty());
        Assertions.assertTrue(skills.contains(ActiveEnum.BERSERK));
        Assertions.assertFalse(skills.contains(ActiveEnum.DOUBLE_ATTACK));
        Assertions.assertTrue(skills.stream().allMatch(
            skill -> ActiveSkillSlots.slotsFor(skill).containsAll(filters)
        ));
    }

    @Test
    void loadsModifierTypesFromCatalog() {
        Assertions.assertEquals(ModifierType.ATTACK, ActiveSkillSlots.typeFor(ActiveEnum.DOUBLE_ATTACK));
        Assertions.assertEquals(ModifierType.DEFENSE, ActiveSkillSlots.typeFor(ActiveEnum.THORNS));
        Assertions.assertEquals(ModifierType.ANY, ActiveSkillSlots.typeFor(ActiveEnum.FEINT));
    }
}
