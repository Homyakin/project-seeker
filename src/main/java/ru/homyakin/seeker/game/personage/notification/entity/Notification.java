package ru.homyakin.seeker.game.personage.notification.entity;

import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandTier;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;

public sealed interface Notification {
    enum RecoveredEnergy implements Notification {
        INSTANCE
    }

    record QuestResult(EventResult.PersonalQuestEventResult value) implements Notification {
    }

    record AutoStartQuest(StartedQuest.Single startedQuest) implements Notification {
    }

    record ContrabandFound(
        Contraband contraband,
        int finderSuccessChancePercent,
        Money sellPrice
    ) implements Notification {
    }

    record ContrabandReceived(
        Contraband contraband,
        Personage finder,
        int receiverSuccessChancePercent
    ) implements Notification {
    }

    record ContrabandEchoSuccess(
        Contraband contraband,
        Personage receiver
    ) implements Notification {
    }

    record ContrabandEchoFailure(
        Contraband contraband,
        Personage receiver
    ) implements Notification {
    }

    record ContrabandExpired(
        ContrabandTier tier
    ) implements Notification {
    }
}
