package ru.homyakin.seeker.models;

import java.time.LocalDateTime;

// TODO добавить дату добавления
public record Chat(
    Long id,
    boolean isActive,
    Language language,
    LocalDateTime lastEventDate
) {
    public Chat copyWithActive(boolean isActive) {
        return new Chat(
            id,
            isActive,
            language,
            lastEventDate
        );
    }
}
