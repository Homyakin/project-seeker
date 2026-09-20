package ru.homyakin.seeker.locale.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.NonNegativeRational;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingCooldowns;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingSkillMath;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;

class BattleLocalizationTest {
    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void legacyDescriptionRemainsUnchanged() {
        assertEquals(
            "• Двойная атака (II, 3 оч.): "
                + "После попадания с шансом 50% наносит 15 дополнительного урона",
            BattleLocalization.skillDescription(Language.RU, ActiveEnum.DOUBLE_ATTACK, 3)
        );
    }

    @Test
    void bothScalingVersionsShowExactEffectForEverySkillAndPoint() {
        assertEquals(15, ActiveEnum.values().length);
        for (final var version : List.of(
            SkillFormulaVersion.SCALING_SKILLS_V1,
            SkillFormulaVersion.SCALING_SKILLS_V2
        )) {
            for (final var skill : ActiveEnum.values()) {
                for (int points = 1; points <= 9; points++) {
                    final var description = BattleLocalization.skillDescription(
                        Language.RU,
                        skill,
                        points,
                        version
                    );
                    assertFalse(description.contains("${"), version + " " + skill + " p=" + points);
                    assertScalingValue(version, skill, points, description);
                }
            }
        }
    }

    @Test
    void cooldownDescriptionsFollowCurrentSchedules() {
        assertTrue(v2Description(ActiveEnum.HIT_AND_RUN, 3).contains("перезарядка: 9"));
        assertTrue(v2Description(ActiveEnum.HIT_AND_RUN, 5).contains("перезарядка: 7"));
        assertTrue(v2Description(ActiveEnum.HIT_AND_RUN, 7).contains("перезарядка: 5"));
        assertFalse(v2Description(ActiveEnum.HIT_AND_RUN, 3).contains("↔"));
        assertTrue(v1Description(ActiveEnum.HIT_AND_RUN, 3).contains("4↔5 (сначала 4)"));
        assertTrue(v2Description(ActiveEnum.GUARD, 3).contains("5↔6 (сначала 5)"));
        assertTrue(v2Description(ActiveEnum.GUARD, 5).contains("4↔5 (сначала 4)"));
        assertTrue(v2Description(ActiveEnum.PENETRATION, 7).contains("3↔4 (сначала 3)"));
    }

    @Test
    void ninthPointShowsEighthPointEffectAndExcessWarningForEverySkill() {
        for (final var skill : ActiveEnum.values()) {
            final var description = v2Description(skill, 9);
            assertScalingValue(SkillFormulaVersion.SCALING_SKILLS_V2, skill, 8, description);
            assertTrue(
                description.contains("Учитываются только 8 из 9 очков"),
                () -> skill + " must warn about its ninth point"
            );
        }
    }

    @Test
    void battleStatsUsesVersionedSkillSnapshotsInsteadOfPassedItems() {
        final var personage = BattlePersonage.forScalingSkills(
            baseItems(),
            Position.FRONT,
            Map.of(ActiveEnum.DOUBLE_ATTACK, 3),
            SkillFormulaVersion.SCALING_SKILLS_V2
        );

        final var stats = BattleLocalization.battleStats(Language.RU, personage, List.of());

        assertTrue(stats.contains("Двойная атака (II, 3 оч.)"));
        assertTrue(stats.contains("15% сохранённой атаки"));
        assertFalse(stats.contains("15 дополнительного урона"));
    }

    private static String v1Description(ActiveEnum skill, int points) {
        return BattleLocalization.skillDescription(
            Language.RU,
            skill,
            points,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );
    }

    private static String v2Description(ActiveEnum skill, int points) {
        return BattleLocalization.skillDescription(
            Language.RU,
            skill,
            points,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );
    }

    private static void assertScalingValue(
        SkillFormulaVersion version,
        ActiveEnum skill,
        int points,
        String description
    ) {
        final var multiplier = ScalingSkillMath.multiplier(points);
        final boolean v2 = version == SkillFormulaVersion.SCALING_SKILLS_V2;
        final var expected = switch (skill) {
            case COUNTER_ATTACK -> percent(multiplier, 30) + "% своей атаки";
            case THORNS -> exact(multiplier.multiply(5).divide(NonNegativeRational.of(4)))
                + "% максимального здоровья и "
                + fraction(multiplier.divide(NonNegativeRational.of(6)))
                + " атаки ближнего боя";
            case DOUBLE_ATTACK -> coefficientPercent(points, v2 ? 100 : 50) + "% сохранённой атаки";
            case BERSERK -> "базовую атаку на " + percent(multiplier, 25) + "%";
            case HIT_AND_RUN -> cooldown(ScalingCooldowns.hitAndRun(version, points).orElseThrow());
            case BLEEDING -> percent(multiplier, 25) + "% сохранённой атаки в ход";
            case KNOCKBACK, RETREAT -> "с шансом " + percent(multiplier, 50) + "%";
            case SELF_HEAL -> "восстанавливает " + percent(multiplier, 5) + "% максимального здоровья";
            case PRECISE_STRIKE -> percent(multiplier, 60) + "% сохранённой атаки";
            case FEINT -> percent(multiplier, 50) + "% своей атаки";
            case GUARD -> cooldown(
                ScalingCooldowns.guardOrPenetration(version, points).orElseThrow()
            );
            case PENETRATION -> v2
                ? coefficientPercent(points, 12) + "% сохранённой атаки"
                : "любой живой цели";
            case ACCUMULATION -> coefficientPercent(points, v2 ? 28 : 25) + "% сохранённой атаки";
            case TEMPO_BREAK -> "при L=100 — до " + ScalingSkillMath.roundHalfUpToInt(
                NonNegativeRational.of(
                    ScalingSkillMath.multiplierNumerator(points) * 100L,
                    v2 ? 5 : 20
                )
            );
        };
        assertTrue(
            description.contains(expected),
            () -> version + " " + skill + " p=" + points + ": " + description
        );
    }

    private static String cooldown(ScalingCooldowns.CooldownSchedule schedule) {
        if (schedule.alternates()) {
            return schedule.firstCooldown()
                + "↔"
                + schedule.secondCooldown()
                + " (сначала "
                + schedule.firstCooldown()
                + ")";
        }
        return Integer.toString(schedule.firstCooldown());
    }

    private static String percent(NonNegativeRational multiplier, int basePercent) {
        return exact(multiplier.multiply(basePercent));
    }

    private static String coefficientPercent(int points, int denominator) {
        return mixedNumber(NonNegativeRational.of(
            ScalingSkillMath.multiplierNumerator(points) * 100L,
            denominator
        ));
    }

    private static String exact(NonNegativeRational value) {
        return new BigDecimal(value.numerator())
            .divide(new BigDecimal(value.denominator()))
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',');
    }

    private static String fraction(NonNegativeRational value) {
        if (value.denominator().equals(BigInteger.ONE)) {
            return value.numerator().toString();
        }
        return value.numerator() + "/" + value.denominator();
    }

    private static String mixedNumber(NonNegativeRational value) {
        final var parts = value.numerator().divideAndRemainder(value.denominator());
        if (parts[1].signum() == 0) {
            return parts[0].toString();
        }
        final var remainder = parts[1] + "/" + value.denominator();
        if (parts[0].signum() == 0) {
            return remainder;
        }
        return parts[0] + " " + remainder;
    }

    private static List<Item> baseItems() {
        return List.of(
            Item.weapon(
                AttackType.SLASH,
                1,
                100,
                new Modifier(ActiveEnum.DOUBLE_ATTACK),
                ItemRarity.COMMON
            ),
            Item.armor(
                DefenseType.CLOTH,
                20,
                1_000,
                new Modifier(ActiveEnum.THORNS),
                ItemRarity.COMMON
            ),
            Item.stats(10, 5, 0.5, 100, 10)
        );
    }
}
