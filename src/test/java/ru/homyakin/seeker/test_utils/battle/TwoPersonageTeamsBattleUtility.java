package ru.homyakin.seeker.test_utils.battle;

import java.util.List;

import ru.homyakin.seeker.game.battle.v3.BattlePersonage;
import ru.homyakin.seeker.game.battle.v3.two_team.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.battle.v3.two_team.TwoTeamBattleWinner;

public class TwoPersonageTeamsBattleUtility {
    private static final int REPEAT = 10;
    private static final TwoPersonageTeamsBattle battle = new TwoPersonageTeamsBattle();

    public static double probabilityOfFirstTeamWin(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        int firstTeamWins = 0;
        for (int i = 0; i < REPEAT; ++i) {
            final var result = battle.battle(
                firstTeam.stream().map(BattlePersonage::clone).toList(),
                secondTeam.stream().map(BattlePersonage::clone).toList()
            );
            if (result.winner() == TwoTeamBattleWinner.FIRST_TEAM) {
                ++firstTeamWins;
            }
        }

        return (double) firstTeamWins / REPEAT;
    }
}
