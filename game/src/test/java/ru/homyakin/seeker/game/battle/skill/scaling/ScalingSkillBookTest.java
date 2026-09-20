package ru.homyakin.seeker.game.battle.skill.scaling;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

class ScalingSkillBookTest {
    @Test
    void newCooldownDoesNotLoseTurnImmediately() {
        final var book = new ScalingSkillBook(Map.of(ActiveEnum.SELF_HEAL, 4));
        final var coolingBeforeActivation = book.coolingAtTurnStart();

        book.startCooldown(ActiveEnum.SELF_HEAL, 2);
        book.finishTurn(coolingBeforeActivation);

        Assertions.assertEquals(2, book.cooldown(ActiveEnum.SELF_HEAL));
    }

    @Test
    void existingCooldownDecreasesOnCompletedTurn() {
        final var book = new ScalingSkillBook(Map.of(ActiveEnum.SELF_HEAL, 4));
        book.startCooldown(ActiveEnum.SELF_HEAL, 2);

        book.finishTurn(book.coolingAtTurnStart());

        Assertions.assertEquals(1, book.cooldown(ActiveEnum.SELF_HEAL));
    }

    @Test
    void alternatingCooldownStartsWithLowerValue() {
        final var book = new ScalingSkillBook(Map.of(ActiveEnum.HIT_AND_RUN, 3));

        Assertions.assertEquals(4, book.startAlternatingCooldown(ActiveEnum.HIT_AND_RUN, 4, 5));
        Assertions.assertEquals(5, book.startAlternatingCooldown(ActiveEnum.HIT_AND_RUN, 4, 5));
        Assertions.assertEquals(4, book.startAlternatingCooldown(ActiveEnum.HIT_AND_RUN, 4, 5));
    }

    @Test
    void pointsAreSaturatedAtEight() {
        final var book = new ScalingSkillBook(Map.of(ActiveEnum.ACCUMULATION, 99));

        Assertions.assertEquals(8, book.points(ActiveEnum.ACCUMULATION));
    }

    @Test
    void remembersFormulaVersionForEachSkill() {
        final var book = new ScalingSkillBook(
            Map.of(ActiveEnum.DOUBLE_ATTACK, 3, ActiveEnum.TEMPO_BREAK, 5),
            Map.of(
                ActiveEnum.DOUBLE_ATTACK, SkillFormulaVersion.SCALING_SKILLS_V1,
                ActiveEnum.TEMPO_BREAK, SkillFormulaVersion.SCALING_SKILLS_V2
            )
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V1,
                book.version(ActiveEnum.DOUBLE_ATTACK)
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                book.version(ActiveEnum.TEMPO_BREAK)
            )
        );
    }
}
