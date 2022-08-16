package ru.homyakin.seeker.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramUtils {
    public static boolean needToProcessUpdate(Update update) {
        return update.hasMyChatMember();
    }

    public static SendMessage createSendMessage(Long chatId, String text) {
        return SendMessage
            .builder()
            .chatId(chatId)
            .text(text)
            .build();
    }
}
