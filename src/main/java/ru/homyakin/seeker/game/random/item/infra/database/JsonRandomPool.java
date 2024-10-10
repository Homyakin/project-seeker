package ru.homyakin.seeker.game.random.item.infra.database;

import java.util.Queue;

public record JsonRandomPool<T>(
    Queue<T> pool
) {
}
