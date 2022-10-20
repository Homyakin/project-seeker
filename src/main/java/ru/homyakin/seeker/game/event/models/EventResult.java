package ru.homyakin.seeker.game.event.models;

public abstract sealed class EventResult {
    public static final class Success extends EventResult {
    }

    public static final class Failure extends EventResult {
    }

    public static final class Unknown extends EventResult {
    }
}
