package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import ru.homyakin.seeker.game.battle.v4.skill.ActiveSkill;

public class SkillMapper {
    public static ActiveSkill map(ActiveEnum activeEnum, int points) {
        return switch (activeEnum) {
            case COUNTER_ATTACK -> new CounterAttack(points);
            case THORNS -> new Thorns(points);
            case DOUBLE_ATTACK -> new DoubleAttack(points);
            case BERSERK -> new Berserk(points);
            case HIT_AND_RUN -> new HitAndRun(points);
            case BLEEDING -> new Bleeding(points);
            case KNOCKBACK -> new Knockback(points);
            case SELF_HEAL -> new SelfHeal(points);
            case PRECISE_STRIKE -> new PreciseStrike(points);
            case RETREAT -> new Retreat(points);
            case FEINT -> new Feint(points);
        };
    }
}
