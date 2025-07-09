package ru.homyakin.seeker.game.battle.v3.two_team;

import ru.homyakin.seeker.game.battle.v3.TeamResult;

public record TwoTeamBattleResult(
    TwoTeamBattleWinner winner,
    TeamResult firstTeamResults,
    TeamResult secondTeamResults
) {
}
