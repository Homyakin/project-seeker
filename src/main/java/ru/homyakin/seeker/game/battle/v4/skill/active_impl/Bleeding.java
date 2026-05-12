package ru.homyakin.seeker.game.battle.v4.skill.active_impl;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.AttackType;
import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.effect.PeriodicDamageEffect;
import ru.homyakin.seeker.game.battle.v4.skill.AttackPowerSkill;
import ru.homyakin.seeker.game.battle.v4.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.v4.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.v4.skill.SkillRank;
import ru.homyakin.seeker.utils.RandomUtils;

public class Bleeding implements DamageDealSkill.OnHitSkill, AttackPowerSkill {
    private static final int CHANCE = 60;
    private static final int TURNS = 4;
    private static final int COOLDOWN = 4;
    private static final AttackType ATTACK_TYPE = AttackType.SLASH;
    private final SkillRank rank;
    private final int attack;
    private int wait = COOLDOWN;

    public Bleeding(int points) {
        this.rank = SkillRank.forPoints(points);
        this.attack = switch (rank) {
            case FIRST -> 5;
            case SECOND -> 10;
            case THIRD -> 15;
            case FOURTH -> 20;
            case FIFTH -> 25;
        };
    }

    @Override
    public double skillPowerRating(SkillPowerInputs inputs) {
        double avgHitsPerProc = COOLDOWN + (100.0 / CHANCE);
        return attack * TURNS * (1 - inputs.dodgeChancePercent() / 100.0) / avgHitsPerProc;
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
