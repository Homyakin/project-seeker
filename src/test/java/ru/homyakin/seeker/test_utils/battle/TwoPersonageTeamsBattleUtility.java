package ru.homyakin.seeker.test_utils.battle;

import java.util.List;
import ru.homyakin.seeker.game.battle.two_team.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.battle.two_team.TwoTeamBattleWinner;
import ru.homyakin.seeker.game.personage.models.Personage;

public class TwoPersonageTeamsBattleUtility {
    private static final int REPEAT = 10;
    private static final TwoPersonageTeamsBattle battle = new TwoPersonageTeamsBattle();

    public static double probabilityOfFirstTeamWin(List<Personage> firstTeam, List<Personage> secondTeam) {
        int firstTeamWins = 0;
        for (int i = 0; i < REPEAT; ++i) {
            final var result = battle.battle(
                firstTeam.stream().map(Personage::toBattlePersonage).toList(),
                secondTeam.stream().map(Personage::toBattlePersonage).toList()
            );
            if (result.winner() == TwoTeamBattleWinner.FIRST_TEAM) {
                ++firstTeamWins;
            }
        }

        return (double) firstTeamWins / REPEAT;
    }
}
