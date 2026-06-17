package ru.homyakin.seeker.utils.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public record IntRange(
    int min,
    int max
) {
    public IntRange(String str) {
        this(
            Integer.parseInt(str.split("-")[0]),
            Integer.parseInt(str.split("-")[1])
        );
    }

    /**
     * @param str Передаётся в формате min-max, например "100-300"
     */
    @JsonCreator
    public static IntRange fromString(String str) {
        final var split = str.split("-");
        return new IntRange(
            Integer.parseInt(split[0]),
            Integer.parseInt(split[1])
        );
    }

    public static IntRange of(int min, int max) {
        return new IntRange(min, max);
    }
}
