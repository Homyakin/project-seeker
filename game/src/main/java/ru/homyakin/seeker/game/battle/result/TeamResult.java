package ru.homyakin.seeker.game.battle.result;

import java.util.List;

public record TeamResult(
    List<GroupBattleResult> groupResults,
    List<PersonageBattleResult> personageResults
) {
}
