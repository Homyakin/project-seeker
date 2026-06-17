package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.event.models.EventType;

public enum BattleType {
    RAID(EventType.RAID),
    WORLD_RAID(EventType.WORLD_RAID),
    ;

    private final EventType eventType;

    BattleType(EventType eventType) {
        this.eventType = eventType;
    }

    public int id() {
        return eventType.id();
    }
}
