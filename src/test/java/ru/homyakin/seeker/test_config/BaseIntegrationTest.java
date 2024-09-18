package ru.homyakin.seeker.test_config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}",
        "spring.datasource.username=${DB_USERNAME}",
        "spring.datasource.password=${DB_PASSWORD}"
    }
)
@ContextConfiguration(classes = TestContainersConfiguration.class)
@ActiveProfiles(value = {"integration-test"})
public abstract class BaseIntegrationTest {
    @MockBean
    private TelegramBotsLongPollingApplication telegramBotsLongPollingApplication;

    @MockBean
    private TelegramSender telegramSender;

    static {
        LocalizationInitializer.initLocale();
    }

    private static final AtomicLong userId = new AtomicLong(0);

    protected UserId nextUserId() {
        return UserId.from(userId.getAndIncrement());
    }
}
