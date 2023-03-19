package ru.homyakin.seeker.telegram.models;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.TelegramBotConfig;

public enum MessageOwner {
    USER,
    THIS_BOT,
    DIFFERENT_BOT;

    public static MessageOwner from(Message message) {
        if (!message.getFrom().getIsBot()) {
            return USER;
        }

        if (TelegramBotConfig.username().equals(message.getFrom().getUserName())) {
            return THIS_BOT;
        }
        return DIFFERENT_BOT;
    }

}
