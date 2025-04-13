package ru.homyakin.seeker.game.battle.two_team;

import ru.homyakin.seeker.game.battle.TeamResult;

public record TwoTeamBattleResult(
    TwoTeamBattleWinner winner,
    TeamResult firstTeamResults,
    TeamResult secondTeamResults
) {
}
