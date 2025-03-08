package ru.homyakin.seeker.game.personage.notification.entity;

import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;

public sealed interface Notification {
    enum RecoveredEnergy implements Notification {
        INSTANCE
    }

    record SuccessQuestResult(EventResult.PersonalQuestResult.Success value) implements Notification {
    }

    record FailureQuestResult(EventResult.PersonalQuestResult.Failure value) implements Notification {
    }

    record AutoStartQuest(StartedQuest startedQuest) implements Notification {
    }
}
