package ru.homyakin.seeker.game.event.personal_quest.model;

public sealed interface TakeQuestError {
    enum PersonageLocked implements TakeQuestError {
        INSTANCE
    }

    record NotEnoughEnergy(int requiredEnergy) implements TakeQuestError {
    }

    enum PersonageInOtherEvent implements TakeQuestError {
        INSTANCE
    }

    enum NoQuests implements TakeQuestError {
        INSTANCE
    }

    enum NotPositiveCount implements TakeQuestError {
        INSTANCE
    }
}
