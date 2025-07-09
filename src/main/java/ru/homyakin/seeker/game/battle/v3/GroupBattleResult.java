package ru.homyakin.seeker.game.battle.v3;

/**
 * Статистика - привилегия зарегистрированных групп, поэтому с тэгом
 */
public record GroupBattleResult(
    String tag,
    GroupBattleStats stats
) {
}
