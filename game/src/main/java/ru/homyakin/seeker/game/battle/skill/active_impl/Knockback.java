package ru.homyakin.seeker.game.battle.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.skill.MovePowerSkill;
import ru.homyakin.seeker.game.battle.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.utils.RandomUtils;

public class Knockback implements DamageDealSkill.OnCritSkill, MovePowerSkill {
    private static final int RANGE = 1;
    private final SkillRank rank;
    private final int chance;

    public Knockback(int points) {
        this.rank = SkillRank.forPoints(points);
        this.chance = switch (rank) {
            case FIRST -> 10;
            case SECOND -> 20;
            case THIRD -> 30;
            case FOURTH -> 40;
            case FIFTH -> 50;
        };
    }

    @Override
    public double skillPowerRating(SkillPowerInputs inputs) {
        return 0;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round) {
        if (!target.isAlive() || !RandomUtils.processChance(chance)) {
            return List.of();
        }
        final int before = target.currentPosition();
        context.moveBackward(target, RANGE);
        final int after = target.currentPosition();
        if (before == after) {
            return List.of();
        }
        return List.of(new BattleEvent.PersonageForcedMove(target.id(), self.id(), ActiveEnum.KNOCKBACK, after, round));
    }
}
