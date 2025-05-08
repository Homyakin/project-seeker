package ru.homyakin.seeker.game.personage.notification.entity;

import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;

public sealed interface Notification {
    enum RecoveredEnergy implements Notification {
        INSTANCE
    }

    record QuestResult(EventResult.PersonalQuestEventResult value) implements Notification {
    }

    record AutoStartQuest(StartedQuest.Single startedQuest) implements Notification {
    }
}
