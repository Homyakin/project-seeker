package ru.homyakin.seeker.telegram.utils;

import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

public class TelegramMethods {
    public static SendMessage createSendMessage(Long chatId, String text) {
        return createSendMessage(chatId, text, null, null);
    }

    public static SendMessage createSendMessage(Long chatId, String text, ReplyKeyboard keyboard) {
        return createSendMessage(chatId, text, null, keyboard);
    }

    public static SendMessage createSendMessage(Long chatId, String text, Integer replyMessageId) {
        return createSendMessage(chatId, text, replyMessageId, null);
    }

    public static SendMessage createSendMessage(Long chatId, String text, Integer replyMessageId, ReplyKeyboard keyboard) {
        return SendMessage
            .builder()
            .chatId(chatId)
            .text(EmojiParser.parseToUnicode(text))
            .replyMarkup(keyboard)
            .parseMode(ParseMode.HTML)
            .replyToMessageId(replyMessageId)
            .disableWebPagePreview(true)
            .build();
    }

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

    public static EditMessageText createEditMessageText(Long chatId, Integer messageId, String text) {
        return createEditMessageText(chatId, messageId, text, null);
    }

    public static EditMessageText createEditMessageText(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        return EditMessageText
            .builder()
            .chatId(chatId)
            .messageId(messageId)
            .text(EmojiParser.parseToUnicode(text))
            .parseMode(ParseMode.HTML)
            .replyMarkup(keyboard)
            .disableWebPagePreview(true)
            .build();
    }

}
