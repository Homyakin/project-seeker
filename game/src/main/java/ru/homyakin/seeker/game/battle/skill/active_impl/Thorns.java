package ru.homyakin.seeker.game.battle.skill.active_impl;

import ru.homyakin.seeker.game.item.models.AttackType;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.AttackPowerSkill;
import ru.homyakin.seeker.game.battle.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.skill.SkillRank;

public class Thorns implements DamageDealSkill.OnDamageReceiveSkill, AttackPowerSkill {
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
        return attack * (1.0 - inputs.dodgeChancePercent() / 100.0);
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round) {
        if (!target.isAlive()) {
            return List.of();
        }
        return target.applySkillDamage(AttackType.PIERCE, attack, self, ActiveEnum.THORNS, round);
    }
}
