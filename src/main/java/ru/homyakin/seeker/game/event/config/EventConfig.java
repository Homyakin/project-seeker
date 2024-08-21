package ru.homyakin.seeker.game.event.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("homyakin.seeker.event")
public record EventConfig(
    Duration raidDuration
) {
}
