package ru.homyakin.seeker.game.event.personal_quest.model;

import java.time.Duration;

public record PersonalQuestRequirements(
    int requiredEnergy,
    Duration requiredTime
) {
}
