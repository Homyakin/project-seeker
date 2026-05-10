package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.AttackType;
import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.effect.PeriodicDamageEffect;
import ru.homyakin.seeker.game.battle.v4.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.utils.RandomUtils;

public class Bleeding implements DamageDealSkill.OnHitSkill {
    private static final int CHANCE = 30;
    private static final int TURNS = 3;
    private static final int COOLDOWN = 3;
    private static final AttackType ATTACK_TYPE = AttackType.SLASH;
    private final SkillRank rank;
    private final int attack;
    private int wait = COOLDOWN;

    public Bleeding(int points) {
        this.rank = SkillRank.forPoints(points);
        this.attack = switch (rank) {
            case FIRST -> 4;
            case SECOND -> 6;
            case THIRD -> 8;
            case FOURTH -> 10;
            case FIFTH -> 12;
        };
    }

    @Override
    public double skillPowerRating(SkillPowerInputs inputs) {
        final double procChance = CHANCE / 100.0;
        final double avgHitsBetweenAttempts = 1.0 + COOLDOWN;
        return attack * TURNS * procChance / avgHitsBetweenAttempts;
    }

    @Override
    public List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round) {
        if (wait > 0) {
            --wait;
            return List.of();
        }
        if (!target.isAlive() || !RandomUtils.processChance(CHANCE)) {
            return List.of();
        }
        wait = COOLDOWN;
        target.addPeriodicDamageEffect(
            new PeriodicDamageEffect(ATTACK_TYPE, attack, 1, TURNS, self.id(), ActiveEnum.BLEEDING)
        );
        return List.of();
    }
}
