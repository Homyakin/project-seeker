package ru.homyakin.seeker.game.event.models;

import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestResult;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleInfo;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.GroupWorldRaidBattleResult;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.PersonageWorldRaidBattleResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;

import java.util.List;

public sealed interface EventResult {

    sealed interface RaidResult extends EventResult {
        enum Expired implements RaidResult {
            INSTANCE
        }

        record Completed(
            Status status,
            Raid raid,
            List<PersonageBattleResult> raidNpcResults,
            List<PersonageRaidResult> personageResults,
            List<GeneratedItemResult> generatedItemResults
        ) implements RaidResult {
            public enum Status {
                SUCCESS,
                FAILURE,
            }
        }
    }

    sealed interface PersonalQuestEventResult extends EventResult {
        record Single(
            PersonalQuest quest,
            Personage personage,
            PersonalQuestResult result
        ) implements PersonalQuestEventResult {

        }

        record Multiple(
            Personage personage,
            List<PersonalQuestResult> results
        ) implements PersonalQuestEventResult {
        }
    }

    record WorldRaidBattleResult(
        boolean isWin,
        List<GroupWorldRaidBattleResult> groupResults,
        List<PersonageWorldRaidBattleResult> personageResults,
        WorldRaidBattleInfo remainedInfo
    ) implements EventResult {
    }
}
