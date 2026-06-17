package ru.homyakin.seeker.game.battle.skill;

public sealed interface SkillCondition {
    sealed interface Self {
        record Chance(int percent) implements Self {}
    }

    record EnemyHealthBelow(int percent) implements SkillCondition {
    }

    record EnemyHealthAbove(int percent) implements SkillCondition {
    }
}
