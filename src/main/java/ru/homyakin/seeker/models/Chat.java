package ru.homyakin.seeker.models;

import java.time.LocalDateTime;

// TODO добавить дату добавления
public record Chat(
    Long id,
    boolean isActive,
    Language language,
    LocalDateTime lastEventDate
) {
    public boolean isSameLanguage(Language language) {
        return this.language == language;
    }
    public Chat copyWithActive(boolean isActive) {
        return new Chat(
            id,
            isActive,
            language,
            lastEventDate
        );
    }
}
