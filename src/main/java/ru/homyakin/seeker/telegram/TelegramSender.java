package ru.homyakin.seeker.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class TelegramSender extends DefaultAbsSender {
    private static final Logger logger = LoggerFactory.getLogger(TelegramSender.class);
    private final String token;

    protected TelegramSender(TelegramBotConfig botConfig, DefaultBotOptions options) {
        super(options);
        this.token = botConfig.token();
    }

    public void send(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (Exception e) {
            logger.error(
                "Unable send message with text %s to %s".formatted(sendMessage.getText(), sendMessage.getChatId())
            );
        }
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
