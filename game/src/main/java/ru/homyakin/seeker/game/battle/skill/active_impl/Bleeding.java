package ru.homyakin.seeker.game.battle.skill.active_impl;

import ru.homyakin.seeker.game.item.models.AttackType;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.effect.PeriodicDamageEffect;
import ru.homyakin.seeker.game.battle.skill.AttackPowerSkill;
import ru.homyakin.seeker.game.battle.skill.DamageDealSkill;
import ru.homyakin.seeker.game.battle.skill.SkillPowerInputs;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
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
