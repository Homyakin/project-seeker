package ru.homyakin.seeker.game.duel;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
