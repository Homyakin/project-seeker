package ru.homyakin.seeker.game.battle;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("homyakin.seeker.battle-visualizer")
public record BattleVisualizerConfig(String baseUrl) {
    public String battleUrl(long launchedEventId) {
        return baseUrl + launchedEventId;
    }
}
