package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.AttackType;
import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPower;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;

public class Thorns implements DamageDealSkill.OnDamageReceiveSkill {
    private static final int CHANCE = 50;
    private final SkillRank rank;
    private final int attack;

    public Thorns(int points) {
        this.rank = SkillRank.forPoints(points);
        this.attack = switch (rank) {
            case FIRST -> 5;
            case SECOND -> 8;
            case THIRD -> 10;
            case FOURTH -> 12;
            case FIFTH -> 15;
        };
    }

    @Override
    public double skillPowerRating(SkillPowerInputs inputs) {
        final double dodgeP = inputs.dodgeChancePercent() / 100.0;
        // Matches apply(): reflects on each damage receive (CHANCE constant unused in combat).
        return attack * (1.0 - dodgeP) * SkillPower.RECEIVED_DAMAGE_EVENTS_PER_OWN_ATTACK;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round) {
        if (!target.isAlive()) {
            return List.of();
        }
        return target.applySkillDamage(AttackType.PIERCE, attack, self.id(), ActiveEnum.THORNS, round);
    }
}
