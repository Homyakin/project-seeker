package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.game.battle.v4.skill.TurnSkill;
import ru.homyakin.seeker.utils.RandomUtils;

public class SelfHeal implements TurnSkill.TurnEndSkill {
    private static final int CHANCE = 50;
    private final SkillRank rank;
    private final int heal;

    public SelfHeal(int points) {
        this.rank = SkillRank.forPoints(points);
        this.heal = switch (rank) {
            case FIRST -> 10;
            case SECOND -> 15;
            case THIRD -> 20;
            case FOURTH -> 25;
            case FIFTH -> 30;
        };
    }

    @Override
    public double skillPowerRating(SkillPowerInputs inputs) {
        final int maxHp = Math.max(1, inputs.maxHealth());
        return heal * (CHANCE / 100.0) * (400.0 / (400.0 + maxHp));
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, int round) {
        if (!self.isAlive() || !RandomUtils.processChance(CHANCE)) {
            return List.of();
        }
        final int before = self.health();
        self.heal(heal);
        final int healed = self.health() - before;
        if (healed <= 0) {
            return List.of();
        }
        return List.of(new BattleEvent.PersonageHealed(self.id(), ActiveEnum.SELF_HEAL, healed, self.health(), round));
    }
}
