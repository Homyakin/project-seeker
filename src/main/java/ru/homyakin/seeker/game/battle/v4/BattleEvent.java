package ru.homyakin.seeker.game.battle.v4;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

import ru.homyakin.seeker.game.battle.v4.skill.active_impl.ActiveEnum;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BattleEvent.RoundStarted.class,        name = "RoundStarted"),
    @JsonSubTypes.Type(value = BattleEvent.InitiativeAfterTick.class, name = "InitiativeAfterTick"),
    @JsonSubTypes.Type(value = BattleEvent.MovedTowardEnemy.class,    name = "MovedTowardEnemy"),
    @JsonSubTypes.Type(value = BattleEvent.AttackDodged.class,        name = "AttackDodged"),
    @JsonSubTypes.Type(value = BattleEvent.DamageReceived.class,      name = "DamageReceived"),
    @JsonSubTypes.Type(value = BattleEvent.EffectDamage.class,        name = "EffectDamage"),
    @JsonSubTypes.Type(value = BattleEvent.SkillDamage.class,         name = "SkillDamage"),
    @JsonSubTypes.Type(value = BattleEvent.PersonageHealed.class,     name = "PersonageHealed"),
    @JsonSubTypes.Type(value = BattleEvent.PersonageAttackBuffed.class, name = "PersonageAttackBuffed"),
    @JsonSubTypes.Type(value = BattleEvent.PersonageForcedMove.class, name = "PersonageForcedMove"),
    @JsonSubTypes.Type(value = BattleEvent.PersonageDefeated.class,   name = "PersonageDefeated"),
})
public sealed interface BattleEvent permits
    BattleEvent.RoundStarted,
    BattleEvent.InitiativeAfterTick,
    BattleEvent.MovedTowardEnemy,
    BattleEvent.AttackDodged,
    BattleEvent.DamageReceived,
    BattleEvent.EffectDamage,
    BattleEvent.SkillDamage,
    BattleEvent.PersonageHealed,
    BattleEvent.PersonageAttackBuffed,
    BattleEvent.PersonageForcedMove,
    BattleEvent.PersonageDefeated {

    int round();

    record RoundStarted(int round) implements BattleEvent { }

    /**
     * Logged after each tick: {@code gaugeAfterTick} is the gauge value for this step;
     * {@code turnGranted} is true when the gauge crossed the threshold and wrapped (this personage joins the mover pool).
     */
    record InitiativeAfterTick(UUID personageId, int gaugeAfterTick, boolean turnGranted, int round) implements BattleEvent { }

    record MovedTowardEnemy(UUID personageId, int newLineIndex, int round) implements BattleEvent { }

    record AttackDodged(UUID attackerId, UUID targetId, int round) implements BattleEvent { }

    record DamageReceived(
        UUID targetId,
        UUID attackerId,
        DamageRoll roll,
        int damageTaken,
        int remainingHealth,
        int round
    ) implements BattleEvent { }

    /**
     * Damage from a timed effect (no dodge roll). {@code sourceId} is the origin (e.g. applier of a DoT),
     * {@code skill} identifies which skill seeded the effect.
     */
    record EffectDamage(
        UUID targetId,
        UUID sourceId,
        ActiveEnum skill,
        AttackType attackType,
        int damageTaken,
        int remainingHealth,
        int round
    ) implements BattleEvent { }

    /**
     * Direct damage from a skill activation (e.g. DoubleAttack, CounterAttack). No dodge roll.
     */
    record SkillDamage(
        UUID targetId,
        UUID sourceId,
        ActiveEnum skill,
        AttackType attackType,
        int damageTaken,
        int remainingHealth,
        int round
    ) implements BattleEvent { }

    record PersonageHealed(
        UUID personageId,
        ActiveEnum skill,
        int amount,
        int remainingHealth,
        int round
    ) implements BattleEvent { }

    /**
     * Flat attack power was increased by a skill (percent applied to each range/type slice), e.g. Berserk.
     */
    record PersonageAttackBuffed(
        UUID personageId,
        ActiveEnum skill,
        int attackBonusPercent,
        int round
    ) implements BattleEvent { }

    /**
     * A personage was forcibly relocated by a skill (Knockback, Retreat, etc.).
     */
    record PersonageForcedMove(
        UUID personageId,
        UUID sourceId,
        ActiveEnum skill,
        int newLineIndex,
        int round
    ) implements BattleEvent { }

    record PersonageDefeated(UUID personageId, UUID killerId, int round) implements BattleEvent { }
}
