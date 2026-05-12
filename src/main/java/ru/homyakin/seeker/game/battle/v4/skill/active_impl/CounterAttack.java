package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.skill.AttackPowerSkill;
import ru.homyakin.seeker.game.battle.v4.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.utils.RandomUtils;

public class CounterAttack implements DamageDealSkill.OnDamageReceiveSkill, AttackPowerSkill {
    private static final int CHANCE = 50;
    private final SkillRank rank;
    private final int attack;

    public CounterAttack(int points) {
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
        return attack * (CHANCE / 100.0) * (1.0 - inputs.dodgeChancePercent() / 100.0);
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round) {
        if (!RandomUtils.processChance(CHANCE)) {
            return List.of();
        }
        final var attackType = RandomUtils.getRandomElement(self.attackTypes());
        return target.applySkillDamage(attackType, attack, self.id(), ActiveEnum.COUNTER_ATTACK, round);
    }
}
