package ru.homyakin.seeker.telegram.models;

import org.telegram.telegrambots.meta.api.objects.User;
import ru.homyakin.seeker.telegram.TelegramBotConfig;

public enum UserType {
    USER,
    THIS_BOT,
    DIFFERENT_BOT;

    public static UserType from(User user) {
        return from(user.getUserName());
    }

    public static UserType from(String username) {
        if (TelegramBotConfig.username().equals(username)) {
            return THIS_BOT;
        }
        if (username.length() >= 3 &&
            username.substring(username.length() - 3).equalsIgnoreCase("bot")) {
            return DIFFERENT_BOT;
        }
        return USER;
    }
}
