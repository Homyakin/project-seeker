package ru.homyakin.seeker.locale.battle;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.function.Function;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.NonNegativeRational;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingCooldowns;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingSkillMath;
import ru.homyakin.seeker.utils.StringNamedTemplate;

final class ScalingSkillPresentation {
    private ScalingSkillPresentation() {
    }

    static String description(
        ActiveEnum skill,
        int points,
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
            case DOUBLE_ATTACK -> params.put("effect_percent", percent(multiplier, 40));
            case BERSERK -> params.put("effect_percent", percent(multiplier, 25));
            case HIT_AND_RUN -> params.put(
                "cooldown",
                cooldownFormatter.apply(ScalingCooldowns.hitAndRun(points).orElseThrow())
            );
            case BLEEDING -> params.put("effect_percent", percent(multiplier, 25));
            case KNOCKBACK, RETREAT -> params.put("effect_percent", percent(multiplier, 50));
            case SELF_HEAL -> params.put("effect_percent", percent(multiplier, 5));
            case PRECISE_STRIKE -> params.put("effect_percent", percent(multiplier, 60));
            case FEINT -> params.put("effect_percent", percent(multiplier, 50));
            case GUARD, PENETRATION -> params.put(
                "cooldown",
                cooldownFormatter.apply(ScalingCooldowns.guardOrPenetration(points).orElseThrow())
            );
            case ACCUMULATION -> params.put("effect_percent", percent(multiplier, 80));
            case TEMPO_BREAK -> {
                params.put("effect_percent", percent(multiplier, 100));
                params.put("effect_at_100", percent(multiplier, 100));
            }
        }
        return StringNamedTemplate.format(template, params);
    }

    private static String percent(NonNegativeRational multiplier, int basePercent) {
        return exactValue(multiplier.multiply(basePercent));
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
}
