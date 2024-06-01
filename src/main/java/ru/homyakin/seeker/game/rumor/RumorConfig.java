package ru.homyakin.seeker.game.rumor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "homyakin.seeker.rumor")
public class RumorConfig {
    private Duration minimalInterval;
    private Duration maximumInterval;

    public Duration minimalInterval() {
        return minimalInterval;
    }

    public Duration maximumInterval() {
        return maximumInterval;
    }

    public void setMinimalInterval(Duration minimalInterval) {
        this.minimalInterval = minimalInterval;
    }

    public void setMaximumInterval(Duration maximumInterval) {
        this.maximumInterval = maximumInterval;
    }
}
