package ru.homyakin.seeker.telegram.utils;

import com.vdurmont.emoji.EmojiParser;
import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.homyakin.seeker.game.personage.models.Personage;

public class SendMessageBuilder {
    private final SendMessage.SendMessageBuilder builder = SendMessage.builder();
    private final List<MessageEntity> entities = new ArrayList<>();
    private String text = null;

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
        this.text = EmojiParser.parseToUnicode(text);
        this.builder.text(this.text);
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

    public SendMessageBuilder mentionPersonage(Personage personage, long userId, int position) {
        if (this.text == null) {
            throw new IllegalStateException("Text must be present for mention");
        }
        MessageEntityUtils.mentionPersonageInText(this.text, personage, userId, position)
            .ifPresent(this.entities::add);
        this.builder.parseMode(null);
        return this;
    }

    public SendMessage build() {
        builder.entities(entities);
        return this.builder.build();
    }
}
