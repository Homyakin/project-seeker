package ru.homyakin.seeker.locale;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalizationTest {
    final List<String> keys = Arrays.stream(LocalizationKeys.values()).map(Enum::name).toList();

    @Test
    public void resourceRuContainsAllKeys() {
        final var resource = Localization.get(Language.RU);
        for (final var key: keys) {
            Assertions.assertTrue(resource.containsKey(key));
        }
    }

    @Test
    public void resourceEnContainsAllKeys() {
        final var resource = Localization.get(Language.EN);
        for (final var key: keys) {
            Assertions.assertTrue(resource.containsKey(key));
        }
    }
}
