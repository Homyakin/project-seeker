package ru.homyakin.seeker.game.duel.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum DuelStatus {
    WAITING(0),
    DECLINED(1),
    EXPIRED(2),
    FINISHED(3);

    private final int id;

    DuelStatus(int id) {
        this.id = id;
    }

    public static DuelStatus getById(int value) {
        return Optional.ofNullable(map.get(value))
            .orElseThrow(() -> new IllegalStateException("Unknown duel status id"));
    }

    public int id() {
        return id;
    }

    private static final Map<Integer, DuelStatus> map = new HashMap<>() {{
        Arrays.stream(DuelStatus.values()).forEach(it -> put(it.id, it));
    }};
}
