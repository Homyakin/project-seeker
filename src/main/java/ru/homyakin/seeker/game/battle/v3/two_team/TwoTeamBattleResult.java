package ru.homyakin.seeker.game.battle.v3.two_team;

public record TwoTeamBattleResult(
    TwoTeamBattleWinner winner,
    TeamResult firstTeamResults,
    TeamResult secondTeamResults
) {
}
