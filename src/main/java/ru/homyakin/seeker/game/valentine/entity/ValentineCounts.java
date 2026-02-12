package ru.homyakin.seeker.game.valentine.entity;

public record ValentineCounts(int sent, int received) {
    public static final ValentineCounts ZERO = new ValentineCounts(0, 0);
}
