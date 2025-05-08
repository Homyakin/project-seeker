package ru.homyakin.seeker.game.event.personal_quest.model;

import java.time.Duration;

public sealed interface StartedQuest {
    Duration duration();

    int takenEnergy();

    record Single(PersonalQuest quest, Duration duration, int takenEnergy) implements StartedQuest {
    }

    record Multiple(int count, Duration duration, int takenEnergy) implements StartedQuest {
    }
}
