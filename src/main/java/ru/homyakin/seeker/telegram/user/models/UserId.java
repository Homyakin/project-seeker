package ru.homyakin.seeker.telegram.user.models;

public record UserId(long value) {
    public static UserId from(long id) {
        return new UserId(id);
    }
}
