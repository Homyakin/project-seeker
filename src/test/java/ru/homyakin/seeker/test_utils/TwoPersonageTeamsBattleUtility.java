package ru.homyakin.seeker.test_utils;

import java.util.ArrayList;
import java.util.List;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.two_team.TwoPersonageTeamsBattle;

public class TwoPersonageTeamsBattleUtility {
    private static final int REPEAT = 5000;
    private static final TwoPersonageTeamsBattle battle = new TwoPersonageTeamsBattle();

    public static double probabilityOfFirstTeamWin(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        /*int firstTeamWins = 0;
        for (int i = 0; i < REPEAT; ++i) {
            final var result = battle.battle(
                new ArrayList<>(firstTeam.stream().map(BattlePersonage::clone).toList()),
                new ArrayList<>(secondTeam.stream().map(BattlePersonage::clone).toList())
            );
            if (result instanceof TwoPersonageTeamsBattle.Result.FirstTeamWin) {
                ++firstTeamWins;
            }
        }

        return (double) firstTeamWins / REPEAT;
         */
        return 0;
    }
}
