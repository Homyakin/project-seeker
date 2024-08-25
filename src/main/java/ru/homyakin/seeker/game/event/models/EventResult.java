package ru.homyakin.seeker.game.event.models;

import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;

import java.util.List;

public sealed interface EventResult {
    record Raid(
        Status status,
        List<PersonageBattleResult> raidNpcResults,
        List<PersonageRaidResult> personageResults,
        List<GeneratedItemResult> generatedItemResults
    ) implements EventResult {

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public boolean isExpired() {
            return status == Status.EXPIRED;
        }

        public enum Status {
            SUCCESS,
            FAILURE,
            EXPIRED,
        }
    }

}
