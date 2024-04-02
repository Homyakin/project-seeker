package ru.homyakin.seeker.utils.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import ru.homyakin.seeker.utils.RandomUtils;

public record IntRange(
    int min,
    int max
) {
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

    public int value() {
        return RandomUtils.getCharacteristic(min, max);
    }
}
