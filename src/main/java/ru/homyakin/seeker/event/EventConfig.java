package ru.homyakin.seeker.event;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "homyakin.seeker.event")
public class EventConfig {
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
