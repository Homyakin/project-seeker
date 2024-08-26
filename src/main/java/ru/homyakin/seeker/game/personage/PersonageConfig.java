package ru.homyakin.seeker.game.personage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("homyakin.seeker.personage")
public record PersonageConfig(
    Duration energyFullRecovery,
    int raidEnergyCost
) {
}
