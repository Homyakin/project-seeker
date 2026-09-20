package ru.homyakin.seeker.locale.battle;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.function.Function;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.NonNegativeRational;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingCooldowns;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingSkillMath;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.utils.StringNamedTemplate;

final class ScalingSkillPresentation {
    private ScalingSkillPresentation() {
    }

    static String description(
        ActiveEnum skill,
        int points,
        SkillFormulaVersion version,
        String template,
        Function<ScalingCooldowns.CooldownSchedule, String> cooldownFormatter
    ) {
        final var multiplier = ScalingSkillMath.multiplier(points);
        final var params = new HashMap<String, Object>();
        switch (skill) {
            case COUNTER_ATTACK -> params.put("effect_percent", percent(multiplier, 30));
            case THORNS -> {
                params.put(
                    "health_percent",
                    exactValue(multiplier.multiply(5).divide(NonNegativeRational.of(4)))
                );
                params.put(
                    "attack_fraction",
                    fraction(multiplier.divide(NonNegativeRational.of(6)))
                );
            }
            case DOUBLE_ATTACK -> params.put(
                "effect_percent",
                coefficientPercent(points, version == SkillFormulaVersion.SCALING_SKILLS_V2 ? 100 : 50)
            );
            case BERSERK -> params.put("effect_percent", percent(multiplier, 25));
            case HIT_AND_RUN -> params.put(
                "cooldown",
                cooldownFormatter.apply(ScalingCooldowns.hitAndRun(version, points).orElseThrow())
            );
            case BLEEDING -> params.put("effect_percent", percent(multiplier, 25));
            case KNOCKBACK, RETREAT -> params.put("effect_percent", percent(multiplier, 50));
            case SELF_HEAL -> params.put("effect_percent", percent(multiplier, 5));
            case PRECISE_STRIKE -> params.put("effect_percent", percent(multiplier, 60));
            case FEINT -> params.put("effect_percent", percent(multiplier, 50));
            case GUARD -> params.put(
                "cooldown",
                cooldownFormatter.apply(ScalingCooldowns.guardOrPenetration(version, points).orElseThrow())
            );
            case PENETRATION -> {
                params.put(
                    "cooldown",
                    cooldownFormatter.apply(ScalingCooldowns.guardOrPenetration(version, points).orElseThrow())
                );
                if (version == SkillFormulaVersion.SCALING_SKILLS_V2) {
                    params.put("effect_percent", coefficientPercent(points, 12));
                }
            }
            case ACCUMULATION -> params.put(
                "effect_percent",
                coefficientPercent(points, version == SkillFormulaVersion.SCALING_SKILLS_V2 ? 28 : 25)
            );
            case TEMPO_BREAK -> {
                final int denominator = version == SkillFormulaVersion.SCALING_SKILLS_V2 ? 5 : 20;
                params.put("effect_percent", percent(multiplier, 100));
                params.put(
                    "effect_at_100",
                    ScalingSkillMath.roundHalfUpToInt(
                        NonNegativeRational.of(
                            ScalingSkillMath.multiplierNumerator(points) * 100L,
                            denominator
                        )
                    )
                );
            }
        }
        return StringNamedTemplate.format(template, params);
    }

    private static String percent(NonNegativeRational multiplier, int basePercent) {
        return exactValue(multiplier.multiply(basePercent));
    }

    private static String coefficientPercent(int points, int denominator) {
        return mixedNumber(NonNegativeRational.of(
            ScalingSkillMath.multiplierNumerator(points) * 100L,
            denominator
        ));
    }

    private static String exactValue(NonNegativeRational value) {
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
}
