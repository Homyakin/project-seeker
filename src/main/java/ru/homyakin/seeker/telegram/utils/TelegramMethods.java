package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import ru.homyakin.seeker.telegram.user.models.UserId;

public class TelegramMethods {

    public static AnswerCallbackQuery createAnswerCallbackQuery(String callbackId, String text) {
        return AnswerCallbackQuery
            .builder()
            .callbackQueryId(callbackId)
            .text(text)
            .showAlert(true)
            .build();
    }

    public static GetChatMember createGetChatMember(Long chatId, UserId userId) {
        return GetChatMember
            .builder()
            .chatId(chatId)
            .userId(userId.value())
            .build();
    }
}
