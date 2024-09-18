package ru.homyakin.seeker.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.telegram.command.CommandParser;
import ru.homyakin.seeker.telegram.command.CommandProcessor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

@Component
public class TelegramUpdateReceiver implements LongPollingSingleThreadUpdateConsumer {
    private static final Logger logger = LoggerFactory.getLogger(TelegramUpdateReceiver.class);

    private final TelegramBotConfig config;
    private final CommandParser commandParser;
    private final CommandProcessor commandProcessor;
    private final UserService userService;

    public TelegramUpdateReceiver(
        TelegramBotConfig config,
        CommandParser commandParser,
        CommandProcessor commandProcessor,
        UserService userService
    ) {
        this.config = config;
        this.commandParser = commandParser;
        this.commandProcessor = commandProcessor;
        this.userService = userService;
    }

    @Override
    public void consume(Update update) {
        logger.debug("New update: {}", update.toString());
        try {
            // TODO подумать как лучше обновлять username у пользователя
            userService.updateUserInfoFromUpdate(update);
            if (TelegramUtils.needToProcessUpdate(update, config.username())) {
                commandParser.parse(update).ifPresent(commandProcessor::process);
            }
        } catch (Exception e) {
            logger.error("Unknown error during update processing", e);
        }
    }
}
