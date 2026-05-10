package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.game.battle.v4.skill.TurnSkill;

public class Berserk implements TurnSkill.TurnStartSkill {
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
        // One-time attack buff below PERCENT_HP; same numeric attack as increaseAttack / decreaseDefense.
        return attack * 0.45;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, int round) {
        if (wasActive || self.percentHp() > PERCENT_HP) {
            return List.of();
        }
        wasActive = true;
        self.increaseAttack(attack);
        self.decreaseDefense(attack);
        return List.of();
    }
}
