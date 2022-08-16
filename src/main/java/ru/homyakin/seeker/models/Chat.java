package ru.homyakin.seeker.models;

// TODO добавить дату добавления
public record Chat(
    Long id,
    boolean isActive,
    Language language
) {
    public Chat copyWithActive(boolean isActive) {
        return new Chat(
            id,
            isActive,
            language
        );
    }
}
