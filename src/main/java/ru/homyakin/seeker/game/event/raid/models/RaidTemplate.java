package ru.homyakin.seeker.game.event.raid.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum RaidTemplate {
    SINGLE_BOSS(1),
    ENEMY_GROUP(2),
    ;

    private final int id;

    RaidTemplate(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    private static final Map<Integer, RaidTemplate> map = new HashMap<>() {{
        Arrays.stream(RaidTemplate.values()).forEach(it -> put(it.id, it));
    }};

    public static RaidTemplate get(int id) {
        return Optional.ofNullable(map.get(id))
            .orElseThrow(() -> new IllegalStateException("Unexpected raid template id: " + id));
    }
}
