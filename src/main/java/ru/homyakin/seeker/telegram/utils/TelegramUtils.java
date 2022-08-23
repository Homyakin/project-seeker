package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramUtils {
    public static boolean needToProcessUpdate(Update update, String botUsername) {
        if (update.hasMyChatMember()) {
            return true;
        }
        if (update.hasMessage()) {
            return isGroupCommand(update.getMessage(), botUsername) || update.getMessage().isUserMessage();
        }
        if (update.hasCallbackQuery()) {
            return isGroupMessage(update.getCallbackQuery().getMessage())
                || update.getCallbackQuery().getMessage().isUserMessage();
        }
        return false;
    }

    public static boolean isGroupMessage(Message message) {
        return message.isGroupMessage() || message.isSuperGroupMessage();
    }

    private static boolean isGroupCommand(Message message, String botUsername) {
        return isGroupMessage(message)
            && message.hasText()
            && isBotCommand(message.getText(), botUsername);
    }

    private static boolean isBotCommand(String text, String botUsername) {
        return text.startsWith("/") && (!text.contains("@") || text.endsWith(botUsername));
    }
}
