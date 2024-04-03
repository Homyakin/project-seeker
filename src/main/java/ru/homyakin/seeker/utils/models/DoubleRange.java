package ru.homyakin.seeker.utils.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import ru.homyakin.seeker.utils.RandomUtils;

public record DoubleRange(
    double min,
    double max
) {
    /**
     * @param str Передаётся в формате min-max, например "100-300"
     */
    @JsonCreator
    public static DoubleRange fromString(String str) {
        final var split = str.split("-");
        return new DoubleRange(
            Double.parseDouble(split[0]),
            Double.parseDouble(split[1])
        );
    }

    public double value() {
        return RandomUtils.getCharacteristic(min, max);
    }
}
