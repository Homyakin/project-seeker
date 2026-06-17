package ru.homyakin.seeker.infrastructure.init;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("homyakin.seeker.init-game-data")
public class InitGameDataConfig {
    private InitGameDataType type;

    public InitGameDataType type() {
        return type;
    }

    public void setType(InitGameDataType type) {
        this.type = type;
    }
}
