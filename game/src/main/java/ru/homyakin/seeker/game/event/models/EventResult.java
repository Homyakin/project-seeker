package ru.homyakin.seeker.game.event.models;

import java.util.List;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.battle.result.PersonageBattleResult;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPersonageResult;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyReward;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestResult;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidLaunchedBattleInfo;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.GroupWorldRaidBattleResult;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.PersonageWorldRaidBattleResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;

public sealed interface EventResult {

    sealed interface RaidResult extends EventResult {
        enum Expired implements RaidResult {
            INSTANCE
        }

        record Completed(
            Status status,
            Raid raid,
            LaunchedRaidEvent launchedRaidEvent,
            List<PersonageBattleResult> raidNpcResults,
            List<PersonageRaidResult> personageResults,
            List<GeneratedItemResult> generatedItemResults,
            int points
        ) implements RaidResult {
            public boolean isSuccess() {
                return status == Status.SUCCESS;
            }

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
        WorldRaidLaunchedBattleInfo remainedInfo
    ) implements EventResult {
    }

    sealed interface DuelResult extends EventResult {
        enum Expired implements DuelResult {
            INSTANCE
        }

        enum AlreadyFinal implements DuelResult {
            INSTANCE
        }
    }

    sealed interface AnomalyResult extends EventResult {
        enum ExpiredGathering implements AnomalyResult { INSTANCE }

        enum AlreadyFinal implements AnomalyResult { INSTANCE }

        record GatheringStarted(LaunchedEvent launchedEvent) implements AnomalyResult { }

        record BattleFinished(
            long launchedEventId,
            GroupId winnerGroupId,
            GroupId loserGroupId,
            List<AnomalyPersonageResult> winnerResults,
            List<AnomalyPersonageResult> loserResults
        ) implements AnomalyResult { }

        record PveBattleFinished(
            long launchedEventId,
            GroupId initiatorGroupId,
            boolean victory,
            AnomalyReward reward,
            List<AnomalyPersonageResult> personageResults,
            List<BattlePersonageStats> enemyStats
        ) implements AnomalyResult { }
    }
}
