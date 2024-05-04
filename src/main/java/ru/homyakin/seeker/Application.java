package ru.homyakin.seeker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.telegram.TelegramBotConfig;
import ru.homyakin.seeker.telegram.TelegramUpdateReceiver;

@SpringBootApplication
@EnableScheduling
public class Application implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private final TelegramUpdateReceiver telegramUpdateReceiver;
    private final TelegramBotConfig telegramBotConfig;
    private final TelegramBotsLongPollingApplication botsApplication;

    public Application(
        TelegramUpdateReceiver telegramUpdateReceiver,
        TelegramBotConfig telegramBotConfig,
        TelegramBotsLongPollingApplication telegramBotsLongPollingApplication
    ) {
        this.telegramUpdateReceiver = telegramUpdateReceiver;
        this.telegramBotConfig = telegramBotConfig;
        this.botsApplication = telegramBotsLongPollingApplication;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            botsApplication.registerBot(telegramBotConfig.token(), telegramUpdateReceiver);
        } catch (Exception e) {
            logger.error("Can't start TelegramBot", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        LocalizationInitializer.initLocale();

        SpringApplication.run(Application.class, args);
    }
}
