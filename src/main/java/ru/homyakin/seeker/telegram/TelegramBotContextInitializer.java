package ru.homyakin.seeker.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.TelegramUrl;

@Configuration
public class TelegramBotContextInitializer {
    @Bean
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication() {
        return new TelegramBotsLongPollingApplication();
    }

    @Bean
    @ConditionalOnProperty(name = "homyakin.seeker.telegram.local-bot-enabled", havingValue = "true")
    public TelegramUrl telegramUrl(
        @Value("homyakin.seeker.telegram.local-bot-host") String localBotHost,
        @Value("homyakin.seeker.telegram.local-bot-port") int localBotPort,
        @Value("homyakin.seeker.telegram.local-bot-schema") String schema
    ) {
        return TelegramUrl.builder()
            .host(localBotHost)
            .port(localBotPort)
            .schema(schema)
            .testServer(false)
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "homyakin.seeker.telegram.local-bot", havingValue = "false", matchIfMissing = true)
    public TelegramUrl defaultTelegramUrl() {
        return TelegramUrl.DEFAULT_URL;
    }
}
