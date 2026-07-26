package ru.homyakin.seeker.game.event.anomaly.entity;

import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

/**
 * Enemy line layout and skill loadout for safe-mode PvE templates.
 *
 * <p>Used by {@link ru.homyakin.seeker.game.event.anomaly.generator.AnomalySafePveGenerator}
 * to place units on {@link Position} lines and assign {@link ActiveEnum} skills.
 */
public enum AnomalyPveFormation {
    /**
     * One FRONT tank with {@link ActiveEnum#KNOCKBACK}; the rest BACK DPS
     * with {@link ActiveEnum#PRECISE_STRIKE} and extended range.
     */
    STRONG_BACK_LINE,

    /**
     * All units on FRONT with {@link ActiveEnum#BERSERK}.
     */
    STRONG_FRONT_LINE,

    /**
     * One FRONT unit; the rest on MID with {@link ActiveEnum#HIT_AND_RUN}.
     */
    MID_LINE,

    /**
     * Roughly half FRONT with {@link ActiveEnum#THORNS}, half BACK with
     * {@link ActiveEnum#BLEEDING} (front gets {@code ceil(n / 2)} slots).
     */
    SPLIT_WINGS,
    ;
}
