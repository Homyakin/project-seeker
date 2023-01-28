package ru.homyakin.seeker.game.duel.models;

public sealed class CreateDuelError {
    public static final class PersonageAlreadyHasDuel extends CreateDuelError {
    }

    public static final class InitiatingPersonageHasLowHealth extends CreateDuelError {
    }

    public static final class AcceptingPersonageHasLowHealth extends CreateDuelError {
    }
}

