package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

public class TelegramUtils {
    public static boolean needToProcessUpdate(Update update, String botUsername) {
        if (update.hasMyChatMember()) {
            return true;
        }
        if (update.hasMessage()) {
            if (update.getMessage().getMigrateFromChatId() != null) {
                return true;
            }
            return isGroupCommand(update.getMessage(), botUsername) || update.getMessage().isUserMessage();
        }
        if (update.hasCallbackQuery()) {
            return isGroupMessage(update.getCallbackQuery().getMessage())
                || update.getCallbackQuery().getMessage().isUserMessage();
        }
        return false;
    }

    public static boolean isGroupMessage(MaybeInaccessibleMessage message) {
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

    /**
     * @param text Сообщение в формате '/command argument'
     * @return 'argument'
     */
    public static Optional<String> deleteCommand(String text) {
        final var split = text.split(" ", 2);
        return split.length == 2 ? Optional.of(split[1]) : Optional.empty();
    }

    public static Message validateCallbackMessage(CallbackQuery callback) {
        if (callback.getMessage() instanceof Message message) {
            return message;
        }
        throw new IllegalArgumentException("Callback must contain accessible message");
    }
}
