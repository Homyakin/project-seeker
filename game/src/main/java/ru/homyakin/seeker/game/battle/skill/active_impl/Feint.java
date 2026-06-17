package ru.homyakin.seeker.game.battle.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.AttackPowerSkill;
import ru.homyakin.seeker.game.battle.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.utils.RandomUtils;

public class Feint implements DamageDealSkill.OnDodgeSkill, AttackPowerSkill {
    private static final int CHANCE = 80;
    private final SkillRank rank;
    private final int attack;

    public Feint(int points) {
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
        return attack * (CHANCE / 100.0) * (inputs.dodgeChancePercent() / 100.0);
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round) {
        if (!target.isAlive() || !RandomUtils.processChance(CHANCE)) {
            return List.of();
        }
        final var attackType = RandomUtils.getRandomElement(self.attackTypes());
        return target.applySkillDamage(attackType, attack, self.id(), ActiveEnum.FEINT, round);
    }
}
