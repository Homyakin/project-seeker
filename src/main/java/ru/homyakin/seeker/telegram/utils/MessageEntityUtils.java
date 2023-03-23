package ru.homyakin.seeker.telegram.utils;

import com.vdurmont.emoji.EmojiParser;
import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.StringUtils;

public class MessageEntityUtils {
    public static Optional<MessageEntity> mentionPersonageInText(String text, Personage personage, long userId, int position) {
        final var parsedIconName = EmojiParser.parseToUnicode(personage.iconWithName());
        final var parsedIcon = EmojiParser.parseToUnicode(personage.icon());
        return StringUtils.findLastOrNeededEntrance(text, parsedIconName, position)
            .map(it -> MessageEntity.builder()
                .type("text_mention")
                .user(new org.telegram.telegrambots.meta.api.objects.User(userId, "", false))
                .offset(it + parsedIcon.length())
                .length(personage.name().length())
                .build()
            );
    }
}
