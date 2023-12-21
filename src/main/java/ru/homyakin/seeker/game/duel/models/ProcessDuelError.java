package ru.homyakin.seeker.game.duel.models;

public sealed interface ProcessDuelError {
    enum DuelLocked implements ProcessDuelError {
        INSTANCE
    }

    enum DuelIsFinished implements ProcessDuelError {
        INSTANCE
    }
}
