package ru.homyakin.seeker.game.battle.v4;

import java.util.UUID;

public sealed interface BattleEvent permits
    BattleEvent.RoundStarted,
    BattleEvent.InitiativeAfterTick,
    BattleEvent.MovedTowardEnemy,
    BattleEvent.AttackDodged,
    BattleEvent.DamageReceived,
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

    record PersonageDefeated(UUID personageId, UUID killerId, int round) implements BattleEvent { }
}
