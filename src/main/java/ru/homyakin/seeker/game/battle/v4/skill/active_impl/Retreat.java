package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.utils.RandomUtils;

public class Retreat implements DamageDealSkill.OnCritReceiveSkill {
    private static final int RANGE = 1;
    private final SkillRank rank;
    private final int chance;

    public Retreat(int points) {
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
        return chance / 100.0 * (inputs.critChancePercent() / 100.0) * 12.0;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round) {
        if (!self.isAlive() || !RandomUtils.processChance(chance)) {
            return List.of();
        }
        final int before = self.currentPosition();
        context.moveBackward(self, RANGE);
        final int after = self.currentPosition();
        if (before == after) {
            return List.of();
        }
        return List.of(new BattleEvent.PersonageForcedMove(self.id(), target.id(), ActiveEnum.RETREAT, after, round));
    }
}
