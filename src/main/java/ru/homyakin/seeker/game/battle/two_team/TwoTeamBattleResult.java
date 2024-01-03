package ru.homyakin.seeker.game.battle.two_team;

import java.util.List;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;

public record TwoTeamBattleResult(
    TwoTeamBattleWinner winner,
    List<PersonageBattleResult> firstTeamResults,
    List<PersonageBattleResult> secondTeamResults
) {
}
