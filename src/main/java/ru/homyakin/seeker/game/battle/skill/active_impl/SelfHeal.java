package ru.homyakin.seeker.game.battle.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.HealthPowerSkill;
import ru.homyakin.seeker.game.battle.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.game.battle.skill.TurnSkill;
import ru.homyakin.seeker.utils.RandomUtils;

public class SelfHeal implements TurnSkill.TurnEndSkill, HealthPowerSkill {
    private static final int CHANCE = 50;
    private static final int COOLDOWN = 2;
    private final SkillRank rank;
    private final int heal;
    private int wait = 0;

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
        final double procsPerTurn = 1.0 / (COOLDOWN + 100.0 / CHANCE);
        final double healPerTurn = heal * procsPerTurn;

        // Защита от деления на ноль или отрицательного знаменателя
        final double netDps = inputs.expectedDamagePerTurn() - healPerTurn;
        if (netDps <= 0) {
            // Хил перекрывает весь урон — персонаж почти бессмертен
            return inputs.maxHealth();
        }

        return inputs.maxHealth() * healPerTurn / netDps;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, int round) {
        if (wait > 0) {
            --wait;
            return List.of();
        }
        if (!self.isAlive() || !RandomUtils.processChance(CHANCE)) {
            return List.of();
        }
        wait = COOLDOWN;
        final int before = self.health();
        self.heal(heal);
        final int healed = self.health() - before;
        if (healed <= 0) {
            return List.of();
        }
        return List.of(new BattleEvent.PersonageHealed(self.id(), ActiveEnum.SELF_HEAL, healed, self.health(), round));
    }
}
