package ru.homyakin.seeker.game.event.personal_quest.model;

import ru.homyakin.seeker.game.models.Money;

public sealed interface PersonalQuestResult {
    record Success(Money reward) implements PersonalQuestResult {}

    enum Failure implements PersonalQuestResult {
        INSTANCE
    }
}
