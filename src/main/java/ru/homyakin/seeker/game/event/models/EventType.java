package ru.homyakin.seeker.game.event.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum EventType {
    BOSS(1),
    ;

    private final int id;

    EventType(int id) {
        this.id = id;
    }

    private static final Map<Integer, EventType> map = new HashMap<>() {{
        Arrays.stream(EventType.values()).forEach(it -> put(it.id, it));
    }};

    public static EventType get(int id) {
        return Optional.ofNullable(map.get(id))
            .orElseThrow(() -> new IllegalStateException("Unexpected event id: " + id));
    }
}
