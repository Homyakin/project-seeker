package ru.homyakin.seeker.game.battle.skill.scaling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingCooldowns.CooldownSchedule;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingCooldowns.Phase;

class ScalingCooldownsTest {
    @ParameterizedTest
    @CsvSource({
        "1, 11, 11",
        "2, 10, 10",
        "3, 9, 9",
        "4, 8, 8",
        "5, 7, 7",
        "6, 6, 6",
        "7, 5, 5",
        "8, 4, 4",
        "9, 4, 4",
    })
    void mapsV2HitAndRunCooldowns(int points, int first, int second) {
        assertSchedule(
            ScalingCooldowns.hitAndRun(SkillFormulaVersion.SCALING_SKILLS_V2, points).orElseThrow(),
            first,
            second
        );
    }

    @ParameterizedTest
    @CsvSource({
        "1, 6, 6",
        "2, 5, 5",
        "3, 4, 5",
        "4, 4, 4",
        "5, 3, 4",
        "6, 3, 3",
        "7, 2, 3",
        "8, 2, 2",
        "9, 2, 2",
    })
    void preservesV1HitAndRunCooldowns(int points, int first, int second) {
        assertSchedule(
            ScalingCooldowns.hitAndRun(SkillFormulaVersion.SCALING_SKILLS_V1, points).orElseThrow(),
            first,
            second
        );
    }

    @ParameterizedTest
    @CsvSource({
        "1, 7, 7",
        "2, 6, 6",
        "3, 5, 6",
        "4, 5, 5",
        "5, 4, 5",
        "6, 4, 4",
        "7, 3, 4",
        "8, 3, 3",
        "9, 3, 3",
    })
    void mapsGuardAndPenetrationCooldowns(int points, int first, int second) {
        assertSchedule(ScalingCooldowns.guardOrPenetration(points).orElseThrow(), first, second);
    }

    @Test
    void zeroPointsHaveNoCooldownSchedule() {
        Assertions.assertTrue(ScalingCooldowns.hitAndRun(0).isEmpty());
        Assertions.assertTrue(ScalingCooldowns.hitAndRun(SkillFormulaVersion.SCALING_SKILLS_V2, 0).isEmpty());
        Assertions.assertTrue(ScalingCooldowns.guardOrPenetration(-1).isEmpty());
    }

    @Test
    void alternatingPhaseStartsWithLowerValueAndAdvancesOnlyOnActivation() {
        final var schedule = ScalingCooldowns.guardOrPenetration(3).orElseThrow();
        final var initial = Phase.INITIAL;

        final var first = schedule.activate(initial);
        final var second = schedule.activate(first.nextPhase());
        final var third = schedule.activate(second.nextPhase());

        Assertions.assertEquals(5, first.cooldown());
        Assertions.assertEquals(6, second.cooldown());
        Assertions.assertEquals(5, third.cooldown());
        Assertions.assertTrue(third.nextPhase().secondNext());
        Assertions.assertFalse(initial.secondNext());
    }

    @Test
    void sharedPhaseAdvancesEvenWhenCurrentOwnersScheduleIsFixed() {
        final var fixedOwner = ScalingCooldowns.guardOrPenetration(4).orElseThrow();
        final var alternatingOwner = ScalingCooldowns.guardOrPenetration(3).orElseThrow();

        final var first = fixedOwner.activate(Phase.INITIAL);
        final var second = alternatingOwner.activate(first.nextPhase());

        Assertions.assertEquals(5, first.cooldown());
        Assertions.assertEquals(6, second.cooldown());
    }

    private static void assertSchedule(CooldownSchedule schedule, int first, int second) {
        Assertions.assertEquals(first, schedule.firstCooldown());
        Assertions.assertEquals(second, schedule.secondCooldown());
        Assertions.assertEquals(first != second, schedule.alternates());
    }
}
