package ru.homyakin.seeker.game.battle.effect;

import ru.homyakin.seeker.game.item.models.AttackType;

import java.util.UUID;

import ru.homyakin.seeker.game.battle.BattleActionLog;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

/**
 * Deals damage at the start of the victim's own {@code move()}, every {@code intervalOwnTurns} turns, for a fixed number of procs.
 */
public final class PeriodicDamageEffect {
    private final AttackType attackType;
    private final int damagePerTick;
    private final int intervalOwnTurns;
    private final UUID sourceId;
    private final ActiveEnum skill;
    private int movesUntilNext;
    private int ticksRemaining;

    public PeriodicDamageEffect(
        AttackType attackType,
        int damagePerTick,
        int intervalOwnTurns,
        int totalTicks,
        UUID sourceId,
        ActiveEnum skill
    ) {
        if (damagePerTick <= 0) {
            throw new IllegalArgumentException("damagePerTick must be positive");
        }
        if (intervalOwnTurns <= 0) {
            throw new IllegalArgumentException("intervalOwnTurns must be positive");
        }
        if (totalTicks <= 0) {
            throw new IllegalArgumentException("totalTicks must be positive");
        }
        this.attackType = attackType;
        this.damagePerTick = damagePerTick;
        this.intervalOwnTurns = intervalOwnTurns;
        this.sourceId = sourceId;
        this.skill = skill;
        this.movesUntilNext = intervalOwnTurns;
        this.ticksRemaining = totalTicks;
    }

    /**
     * @return true if this effect is exhausted and should be removed
     */
    public boolean tickOnOwnTurnBegin(BattlePersonage victim, BattleActionLog log, int round) {
        if (--movesUntilNext > 0) {
            return false;
        }
        movesUntilNext = intervalOwnTurns;
        victim.applyEffectDamage(attackType, damagePerTick, sourceId, skill, log, round);
        return --ticksRemaining <= 0;
    }
}
