package ru.homyakin.seeker.game.event.models;

import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.models.Money;
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

    sealed interface PersonalQuestResult extends EventResult {
        enum Error implements PersonalQuestResult {
            INSTANCE
        }

        record Success(
            PersonalQuest quest,
            Personage personage,
            Money reward
        ) implements PersonalQuestResult {
        }

        record Failure(
            PersonalQuest quest,
            Personage personage
        ) implements PersonalQuestResult {
        }
    }
}
