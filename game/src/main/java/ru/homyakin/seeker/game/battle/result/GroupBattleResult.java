package ru.homyakin.seeker.game.battle.result;

import ru.homyakin.seeker.game.battle.GroupBattleStats;

/**
 * Статистика - привилегия зарегистрированных групп, поэтому с тэгом
 */
public record GroupBattleResult(
    String tag,
    GroupBattleStats stats
) {
}
