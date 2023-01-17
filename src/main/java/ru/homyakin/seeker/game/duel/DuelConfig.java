package ru.homyakin.seeker.game.duel;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "homyakin.seeker.duel")
public class DuelConfig {
    private Duration lifeTime;

    public Duration lifeTime() {
        return lifeTime;
    }

    public void setLifeTime(Duration lifeTime) {
        this.lifeTime = lifeTime;
    }
}
