package ru.homyakin.seeker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.telegram.TelegramUpdateReceiver;

@SpringBootApplication
@EnableScheduling
public class Application implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private final TelegramUpdateReceiver telegramUpdateReceiver;

    public Application(TelegramUpdateReceiver telegramUpdateReceiver) {
        this.telegramUpdateReceiver = telegramUpdateReceiver;
    }

    @Override
    public void run(String... args) throws Exception {
        final var telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(telegramUpdateReceiver);
    }

    public static void main(String[] args) {
        LocalizationInitializer.initLocale();

        SpringApplication.run(Application.class, args);
    }
}
