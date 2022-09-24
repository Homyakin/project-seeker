package ru.homyakin.seeker.chat;

import java.time.LocalDateTime;
import ru.homyakin.seeker.locale.Language;

// TODO добавить дату добавления
public record Chat(
    Long id,
    boolean isActive,
    Language language,
    LocalDateTime nextEventDate
) {
    public boolean isSameLanguage(Language language) {
        return this.language == language;
    }
}
