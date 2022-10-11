package ru.homyakin.seeker.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotOptions;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import ru.homyakin.seeker.telegram.command.CommandParser;
import ru.homyakin.seeker.telegram.command.CommandProcessor;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

/*
Наследование идёт от LongPollingBot вместо TelegramLongPollingBot, чтобы отдельно имплементировать DefaultAbsSender
и не попадать в цикличную зависимость, когда надо отправить несколько сообщений
 */
@Component
public class TelegramUpdateReceiver implements LongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramUpdateReceiver.class);

    private final TelegramBotConfig config;
    private final DefaultBotOptions botOptions;
    private final CommandParser commandParser;
    private final CommandProcessor commandProcessor;

    public TelegramUpdateReceiver(
        TelegramBotConfig config,
        DefaultBotOptions botOptions,
        CommandParser commandParser,
        CommandProcessor commandProcessor
    ) {
        this.config = config;
        this.botOptions = botOptions;
        this.commandParser = commandParser;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.debug("New update: " + update.toString());
        if (TelegramUtils.needToProcessUpdate(update, getBotUsername())) {
            commandParser.parse(update).ifPresent(commandProcessor::process);
        }
    }

    @Override
    public String getBotUsername() {
        return config.username();
    }

    @Override
    public String getBotToken() {
        return config.token();
    }

    @Override
    public BotOptions getOptions() {
        return botOptions;
    }

    @Override
    public void clearWebhook() throws TelegramApiRequestException {
        /*
         Данный метод обязателен в интерфейсе, чтобы удалить вебхук при регистрации. Но если вебхука для бота никогда
         не создавалось, то метод можно оставить пустым
        */
    }
}
