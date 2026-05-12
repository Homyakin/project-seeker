package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.skill.MovePowerSkill;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.game.battle.v4.skill.TurnSkill;
import ru.homyakin.seeker.utils.RandomUtils;

public class HitAndRun implements TurnSkill.TurnStartSkill, MovePowerSkill {
    private static final int COOLDOWN = 3;
    private static final int RANGE_CHANGE = 1;
    private static final int TURNS = 1;
    private final SkillRank rank;
    private final int chance;
    private int wait = COOLDOWN;

    public HitAndRun(int points) {
        this.rank = SkillRank.forPoints(points);
        this.chance = switch (rank) {
            case FIRST -> 10;
            case SECOND -> 15;
            case THIRD -> 20;
            case FOURTH -> 25;
            case FIFTH -> 30;
        };
    }

    @Override
    public double skillPowerRating(SkillPowerInputs inputs) {
        return 0;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, int round) {
        if (wait > 0) {
            --wait;
            return List.of();
        }
        if (!RandomUtils.processChance(chance)) {
            return List.of();
        }
        self.increaseMaxRange(RANGE_CHANGE, TURNS);
        wait = COOLDOWN;
        return List.of();
    }
}
