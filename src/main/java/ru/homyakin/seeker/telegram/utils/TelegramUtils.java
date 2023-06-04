package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.Chat;
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

    public static boolean isGroupChat(Chat chat) {
        return chat.isGroupChat() || chat.isSuperGroupChat();
    }

    public static boolean isBotCommand(String text, String botUsername) {
        if (!text.startsWith("/")) {
            return false;
        }
        final var command = text.split(" ")[0];
        return (!command.contains("@") || command.split("@")[1].equalsIgnoreCase(botUsername));
    }
}
