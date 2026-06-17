package ru.homyakin.seeker.game.event.personal_quest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.utils.models.IntRange;

import java.time.Duration;

@ConfigurationProperties("homyakin.seeker.event.personal-quest")
public record PersonalQuestConfig(
    int requiredEnergy,
    int requiredEnergyForAutoStart,
    Duration requiredTime,
    int baseSuccessProbability,
    IntRange reward
) {
}
