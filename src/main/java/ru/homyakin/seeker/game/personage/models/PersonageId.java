package ru.homyakin.seeker.game.personage.models;

public record PersonageId(long value) {
    public static PersonageId from(long id) {
        return new PersonageId(id);
    }
}
