package ru.homyakin.seeker.game.event.raid;

import java.util.List;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.event.models.PersonageRaidResult;

public record RaidResult(
    boolean isSuccess,
    List<PersonageBattleResult> raidNpcResults,
    List<PersonageRaidResult> personageResults
) {
}
