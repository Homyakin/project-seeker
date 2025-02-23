package ru.homyakin.seeker.game.personage.models;

public record PersonageId(long value) {
    public static PersonageId from(long id) {
        return new PersonageId(id);
    }

    public static PersonageId from(String id) {
        return new PersonageId(Long.parseLong(id));
    }
}
