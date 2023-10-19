package ru.homyakin.seeker.telegram.utils;

import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public class EditMessageTextBuilder {
    private final EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder();
    private final List<MessageEntity> entities = new ArrayList<>();

    private EditMessageTextBuilder() {
    }

    public static EditMessageTextBuilder builder() {
        final var instance = new EditMessageTextBuilder();
        instance.builder
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true);
        return instance;
    }

    public EditMessageTextBuilder text(String text) {
        this.builder.text(text);
        return this;
    }

    public EditMessageTextBuilder keyboard(InlineKeyboardMarkup keyboard) {
        this.builder.replyMarkup(keyboard);
        return this;
    }

    public EditMessageTextBuilder chatId(long chatId) {
        this.builder.chatId(chatId);
        return this;
    }

    public EditMessageTextBuilder chatId(UserId userId) {
        return chatId(userId.value());
    }

    public EditMessageTextBuilder chatId(GroupId groupId) {
        return chatId(groupId.value());
    }

    public EditMessageTextBuilder messageId(int messageId) {
        this.builder.messageId(messageId);
        return this;
    }

    public EditMessageText build() {
        builder.entities(entities);
        return this.builder.build();
    }
}
