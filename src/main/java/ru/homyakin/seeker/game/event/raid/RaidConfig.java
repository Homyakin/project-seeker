package ru.homyakin.seeker.game.event.raid;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("homyakin.seeker.raid")
public record RaidConfig(
    int energyCost
) {
}
