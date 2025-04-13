package ru.homyakin.seeker.game.event.world_raid.entity;

public enum ActiveWorldRaidStatus implements WorldRaidStatus {
    RESEARCH(1),
    BATTLE(2),
    ;

    private final int id;

    ActiveWorldRaidStatus(int id) {
        this.id = id;
    }

    public static ActiveWorldRaidStatus get(int id) {
        for (final var status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ActiveWorldRaidStatus ID: " + id);
    }

    @Override
    public int id() {
        return id;
    }
}
