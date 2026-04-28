package ru.homyakin.seeker.game.battle.v3.two_team;

/**
 * Статистика - привилегия зарегистрированных групп, поэтому с тэгом
 */
public record GroupBattleResult(
    String tag,
    GroupBattleStats stats
) {
}
