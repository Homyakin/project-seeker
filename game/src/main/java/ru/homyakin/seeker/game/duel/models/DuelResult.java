package ru.homyakin.seeker.game.duel.models;

public record DuelResult(
    DuelPersonageResult winner,
    DuelPersonageResult loser
) {
}
