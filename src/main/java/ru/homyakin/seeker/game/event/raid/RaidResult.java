package ru.homyakin.seeker.game.event.raid;

import java.util.List;
import ru.homyakin.seeker.game.battle.BattlePersonage;

public record RaidResult(
    boolean isSuccess,
    List<BattlePersonage> raidNpcResults,
    List<BattlePersonage> personageResults
) {
}
