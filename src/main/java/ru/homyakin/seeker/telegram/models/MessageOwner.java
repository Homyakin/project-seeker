package ru.homyakin.seeker.telegram.models;

import org.telegram.telegrambots.meta.api.objects.Message;

public enum MessageOwner {
    USER,
    THIS_BOT,
    DIFFERENT_BOT;

    private static String botUsername;

    public static MessageOwner from(Message message) {
        if (!message.getFrom().getIsBot()) {
            return USER;
        }

        if (botUsername.equals(message.getFrom().getUserName())) {
            return THIS_BOT;
        }
        return DIFFERENT_BOT;
    }

    public static void setBotUsername(String username) {
        if (botUsername == null) {
            botUsername = username;
        } else {
            throw new IllegalStateException("Username in MessageOwner is already set!");
        }
    }
}
