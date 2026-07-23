package ru.homyakin.seeker.game.battle.skill;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
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
    void filtersBySlot() {
        final var mainHandSkills = ActiveSkillSlots.sortedSkills(Optional.of(PersonageSlot.MAIN_HAND));
        Assertions.assertFalse(mainHandSkills.isEmpty());
        Assertions.assertTrue(mainHandSkills.stream().allMatch(
            skill -> ActiveSkillSlots.slotsFor(skill).contains(PersonageSlot.MAIN_HAND)
        ));
    }
}
