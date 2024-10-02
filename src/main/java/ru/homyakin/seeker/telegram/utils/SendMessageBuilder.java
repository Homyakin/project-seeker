package ru.homyakin.seeker.telegram.utils;

import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public class SendMessageBuilder {
    @SuppressWarnings("rawtypes")
    private final SendMessage.SendMessageBuilder builder = SendMessage.builder();
    private final List<MessageEntity> entities = new ArrayList<>();

    private SendMessageBuilder() {
    }

    public static SendMessageBuilder builder() {
        final var instance = new SendMessageBuilder();
        instance.builder
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true);
        return instance;
    }

    public SendMessageBuilder text(String text) {
        this.builder.text(text);
        return this;
    }

    public SendMessageBuilder keyboard(ReplyKeyboard keyboard) {
        this.builder.replyMarkup(keyboard);
        return this;
    }

    public SendMessageBuilder replyMessageId(int replyMessageId) {
        this.builder.replyToMessageId(replyMessageId);
        return this;
    }

    public SendMessageBuilder chatId(long chatId) {
        this.builder.chatId(chatId);
        return this;
    }

    public SendMessageBuilder chatId(UserId userId) {
        return chatId(userId.value());
    }

    public SendMessageBuilder chatId(GroupId groupId) {
        return chatId(groupId.value());
    }

    public SendMessage build() {
        builder.entities(entities);
        return this.builder.build();
    }
}
