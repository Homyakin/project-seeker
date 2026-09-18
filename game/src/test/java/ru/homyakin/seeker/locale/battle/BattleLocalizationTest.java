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
    void scalingDescriptionsShowExactEffectForEverySkillAndPoint() {
        assertEquals(15, ActiveEnum.values().length);
        for (final var skill : ActiveEnum.values()) {
            for (int points = 1; points <= 9; points++) {
                final var description = BattleLocalization.skillDescription(
                    Language.RU,
                    skill,
                    points,
                    SkillFormulaVersion.SCALING_SKILLS_V1
                );
                assertFalse(description.contains("${"), skill + " p=" + points);
                assertScalingValue(skill, points, description);
            }
        }
    }

    @Test
    void oddSkillPointsShowAlternatingCooldowns() {
        assertTrue(scalingDescription(ActiveEnum.HIT_AND_RUN, 3).contains("4↔5 (сначала 4)"));
        assertTrue(scalingDescription(ActiveEnum.HIT_AND_RUN, 5).contains("3↔4 (сначала 3)"));
        assertTrue(scalingDescription(ActiveEnum.HIT_AND_RUN, 7).contains("2↔3 (сначала 2)"));
        assertTrue(scalingDescription(ActiveEnum.GUARD, 3).contains("5↔6 (сначала 5)"));
        assertTrue(scalingDescription(ActiveEnum.GUARD, 5).contains("4↔5 (сначала 4)"));
        assertTrue(scalingDescription(ActiveEnum.PENETRATION, 7).contains("3↔4 (сначала 3)"));
    }

    @Test
    void ninthPointShowsEighthPointEffectAndExcessWarningForEverySkill() {
        for (final var skill : ActiveEnum.values()) {
            final var description = scalingDescription(skill, 9);
            assertScalingValue(skill, 8, description);
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
            Map.of(ActiveEnum.DOUBLE_ATTACK, 3)
        );

        final var stats = BattleLocalization.battleStats(Language.RU, personage, List.of());

        assertTrue(stats.contains("Двойная атака (II, 3 оч.)"));
        assertTrue(stats.contains("30% сохранённой атаки"));
        assertFalse(stats.contains("15 дополнительного урона"));
    }

    private static String scalingDescription(ActiveEnum skill, int points) {
        return BattleLocalization.skillDescription(
            Language.RU,
            skill,
            points,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );
    }

    private static void assertScalingValue(ActiveEnum skill, int points, String description) {
        final var multiplier = ScalingSkillMath.multiplier(points);
        final var expected = switch (skill) {
            case COUNTER_ATTACK -> percent(multiplier, 30) + "% своей атаки";
            case THORNS -> exact(multiplier.multiply(5).divide(NonNegativeRational.of(4)))
                + "% максимального здоровья и "
                + fraction(multiplier.divide(NonNegativeRational.of(6)))
                + " атаки ближнего боя";
            case DOUBLE_ATTACK -> percent(multiplier, 40) + "% сохранённой атаки";
            case BERSERK -> "базовую атаку на " + percent(multiplier, 25) + "%";
            case HIT_AND_RUN -> cooldown(ScalingCooldowns.hitAndRun(points).orElseThrow());
            case BLEEDING -> percent(multiplier, 25) + "% сохранённой атаки в ход";
            case KNOCKBACK, RETREAT -> "с шансом " + percent(multiplier, 50) + "%";
            case SELF_HEAL -> "восстанавливает " + percent(multiplier, 5) + "% максимального здоровья";
            case PRECISE_STRIKE -> percent(multiplier, 60) + "% сохранённой атаки";
            case FEINT -> percent(multiplier, 50) + "% своей атаки";
            case GUARD, PENETRATION -> cooldown(
                ScalingCooldowns.guardOrPenetration(points).orElseThrow()
            );
            case ACCUMULATION -> percent(multiplier, 80) + "% сохранённой атаки";
            case TEMPO_BREAK -> percent(multiplier, 100) + "% от L";
        };
        assertTrue(description.contains(expected), () -> skill + " p=" + points + ": " + description);
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
