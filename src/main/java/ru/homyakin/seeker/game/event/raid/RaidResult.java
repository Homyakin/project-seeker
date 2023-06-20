package ru.homyakin.seeker.game.event.raid;

import java.util.List;
import ru.homyakin.seeker.game.battle.PersonageResult;

public record RaidResult(
    boolean isSuccess,
    List<PersonageResult> raidNpcResults,
    List<PersonageResult> personageResults
) {
}
