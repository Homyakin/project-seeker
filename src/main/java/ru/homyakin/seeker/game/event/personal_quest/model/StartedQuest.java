package ru.homyakin.seeker.game.event.personal_quest.model;

import java.time.Duration;

public record StartedQuest(
    PersonalQuest quest,
    Duration duration,
    int takenEnergy
) {
}
