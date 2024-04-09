package ru.homyakin.seeker.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "homyakin.seeker.telegram")
public class TelegramBotConfig {
    private String token;
    private static String username;

    public String token() {
        return token;
    }

    public static String username() {
        return username;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUsername(String username) {
        TelegramBotConfig.username = username;
    }
}
