package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.game.item.models.AttackType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

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
    @JsonSubTypes.Type(value = BattleEvent.TargetSelected.class,      name = "TargetSelected"),
    @JsonSubTypes.Type(value = BattleEvent.AttackIntercepted.class,   name = "AttackIntercepted"),
    @JsonSubTypes.Type(value = BattleEvent.ScalingSkillDamage.class,  name = "ScalingSkillDamage"),
    @JsonSubTypes.Type(value = BattleEvent.SkillChargeChanged.class,  name = "SkillChargeChanged"),
    @JsonSubTypes.Type(value = BattleEvent.InitiativeDelayed.class,   name = "InitiativeDelayed"),
    @JsonSubTypes.Type(value = BattleEvent.ThreatChanged.class,       name = "ThreatChanged"),
    @JsonSubTypes.Type(value = BattleEvent.SkillWindowUsed.class,     name = "SkillWindowUsed"),
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
    BattleEvent.PersonageDefeated,
    BattleEvent.TargetSelected,
    BattleEvent.AttackIntercepted,
    BattleEvent.ScalingSkillDamage,
    BattleEvent.SkillChargeChanged,
    BattleEvent.InitiativeDelayed,
    BattleEvent.ThreatChanged,
    BattleEvent.SkillWindowUsed {

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

    /**
     * Records the single target selection for an ordinary attack attempt. The final target can differ from the
     * originally selected one after an interception.
     */
    record TargetSelected(
        UUID attackerId,
        UUID originalTargetId,
        UUID finalTargetId,
        int distance,
        int round
    ) implements BattleEvent { }

    /**
     * Records an ordinary attack redirected from {@code originalTargetId} to {@code interceptorId}.
     */
    record AttackIntercepted(
        UUID attackerId,
        UUID originalTargetId,
        UUID interceptorId,
        int round
    ) implements BattleEvent { }

    /**
     * One direct or periodic scaling-skill damage application. A mixed attack remains one event and one final value.
     */
    record ScalingSkillDamage(
        UUID targetId,
        UUID sourceId,
        ActiveEnum skill,
        Map<AttackType, Integer> basis,
        int coefficientNumerator,
        int coefficientDenominator,
        boolean periodic,
        int damageTaken,
        int remainingHealth,
        int round
    ) implements BattleEvent {
        public ScalingSkillDamage {
            final var orderedBasis = new EnumMap<AttackType, Integer>(AttackType.class);
            orderedBasis.putAll(basis);
            basis = Collections.unmodifiableMap(orderedBasis);
        }
    }

    record SkillChargeChanged(
        UUID personageId,
        ActiveEnum skill,
        int charges,
        boolean discharged,
        int round
    ) implements BattleEvent { }

    record InitiativeDelayed(
        UUID targetId,
        UUID sourceId,
        ActiveEnum skill,
        int amount,
        int gaugeAfter,
        int round
    ) implements BattleEvent { }

    enum ThreatReason {
        DAMAGE_TAKEN,
        NORMAL_HIT,
        KILL,
    }

    record ThreatChanged(
        UUID personageId,
        UUID sourceId,
        ActiveEnum skill,
        int delta,
        int resultingThreat,
        ThreatReason reason,
        int round
    ) implements BattleEvent { }

    record SkillWindowUsed(UUID personageId, ActiveEnum skill, int round) implements BattleEvent { }
}
