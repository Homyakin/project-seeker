package ru.homyakin.seeker.website.battle;

import java.util.Locale;
import java.util.Optional;

public enum BattleType {
    RAID,
    WORLD_RAID,
    ANOMALY_PVE,
    ANOMALY_PVP,
    DUEL;

    public static Optional<BattleType> fromParam(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(BattleType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
