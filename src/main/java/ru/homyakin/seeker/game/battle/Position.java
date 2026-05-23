package ru.homyakin.seeker.game.battle;

public enum Position {
    FRONT,
    MID,
    BACK,
    ;

    public static Position fromString(String value) {
        return valueOf(value);
    }
}
