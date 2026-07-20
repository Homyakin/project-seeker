package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

public record BattleSkillInitSnapshot(
    ActiveEnum code,
    int points
) {
}
