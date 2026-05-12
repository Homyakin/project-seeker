package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.skill.AttackPowerSkill;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.game.battle.v4.skill.TurnSkill;

public class Berserk implements TurnSkill.TurnStartSkill, AttackPowerSkill {
    private static final int PERCENT_HP = 30;
    private final SkillRank rank;
    private final int attack;
    private boolean wasActive = false;

    public Berserk(int points) {
        this.rank = SkillRank.forPoints(points);
        this.attack = switch (rank) {
            case FIRST -> 10;
            case SECOND -> 15;
            case THIRD -> 20;
            case FOURTH -> 25;
            case FIFTH -> 30;
        };
    }

    @Override
    public double skillPowerRating(SkillPowerInputs inputs) {
        final double totalFightTurns = inputs.maxHealth() / inputs.expectedDamagePerTurn();
        final double turnsUntilActivation = inputs.maxHealth()
            * (1.0 - PERCENT_HP / 100.0)
            / inputs.expectedDamagePerTurn();
        final double activeTurns = totalFightTurns - turnsUntilActivation;
        final double extraDps = inputs.baseAttack() * (attack / 100.0);
        return extraDps * activeTurns / totalFightTurns;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, int round) {
        if (wasActive || self.percentHp() > PERCENT_HP) {
            return List.of();
        }
        wasActive = true;
        self.increaseAttack(attack);
        return List.of(new BattleEvent.PersonageAttackBuffed(self.id(), ActiveEnum.BERSERK, attack, round));
    }
}
