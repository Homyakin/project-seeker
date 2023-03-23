package ru.homyakin.seeker.telegram.utils;

import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;

public class TelegramMethods {

    public static AnswerCallbackQuery createAnswerCallbackQuery(String callbackId, String text) {
        return AnswerCallbackQuery
            .builder()
            .callbackQueryId(callbackId)
            .text(EmojiParser.parseToUnicode(text))
            .showAlert(true)
            .build();
    }

    public static GetChatMember createGetChatMember(Long chatId, Long userId) {
        return GetChatMember
            .builder()
            .chatId(chatId)
            .userId(userId)
            .build();
    }
}
