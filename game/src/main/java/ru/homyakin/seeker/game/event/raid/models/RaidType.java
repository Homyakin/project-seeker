package ru.homyakin.seeker.game.event.raid.models;

public enum RaidType {
    WOLFPACK("wolfpack"),
    ZOMBIE_HORDE("zombie_horde"),
    MYCONID_COLONY("myconid_colony"),
    MAGGEESE_FLOCK("maggeese_flock"),
    ;

    private final String code;

    RaidType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static RaidType fromCode(String code) {
        for (final var raidType : values()) {
            if (raidType.code.equals(code)) {
                return raidType;
            }
        }
        throw new IllegalStateException("Unexpected raid code: " + code);
    }
}
