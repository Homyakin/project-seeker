package ru.homyakin.seeker.game.event.world_raid.entity;

public enum FinalWorldRaidStatus implements WorldRaidStatus {
    FINISHED(3),
    CONTINUED(4),
    ;

    private final int id;

    FinalWorldRaidStatus(int id) {
        this.id = id;
    }

    public static FinalWorldRaidStatus get(int id) {
        for (final var status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid FinalWorldRaidStatus ID: " + id);
    }

    @Override
    public int id() {
        return id;
    }
}
