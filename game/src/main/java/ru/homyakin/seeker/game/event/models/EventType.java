package ru.homyakin.seeker.game.event.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum EventType {
    RAID(1, true),
    PERSONAL_QUEST(2, true),
    WORLD_RAID(3, false),
    ;

    private final int id;
    private final boolean isBlocking;

    EventType(int id, boolean isBlocking) {
        this.id = id;
        this.isBlocking = isBlocking;
    }

    public int id() {
        return id;
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    private static final Map<Integer, EventType> map = new HashMap<>() {{
        Arrays.stream(EventType.values()).forEach(it -> put(it.id, it));
    }};

    public static EventType get(int id) {
        return Optional.ofNullable(map.get(id))
            .orElseThrow(() -> new IllegalStateException("Unexpected event id: " + id));
    }
}
