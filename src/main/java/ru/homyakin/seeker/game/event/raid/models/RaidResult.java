package ru.homyakin.seeker.game.event.raid.models;

import java.util.List;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;

public record RaidResult(
    boolean isSuccess,
    List<PersonageBattleResult> raidNpcResults,
    List<PersonageRaidResult> personageResults
) {
}
