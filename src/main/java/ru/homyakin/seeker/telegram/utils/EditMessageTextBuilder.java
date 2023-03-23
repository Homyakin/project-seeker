package ru.homyakin.seeker.telegram.utils;

import com.vdurmont.emoji.EmojiParser;
import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.personage.models.Personage;

public class EditMessageTextBuilder {
    private final EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder();
    private final List<MessageEntity> entities = new ArrayList<>();
    private String text = null;

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
        this.text = EmojiParser.parseToUnicode(text);
        this.builder.text(this.text);
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

    public EditMessageTextBuilder messageId(int messageId) {
        this.builder.messageId(messageId);
        return this;
    }

    public EditMessageTextBuilder mentionPersonage(Personage personage, long userId, int position) {
        if (this.text == null) {
            throw new IllegalStateException("Text must be present for mention");
        }
        this.entities.add(MessageEntityUtils.mentionPersonageInText(this.text, personage, userId, position));
        this.builder.parseMode(null);
        return this;
    }

    public EditMessageText build() {
        builder.entities(entities);
        return this.builder.build();
    }
}
