package ru.homyakin.seeker.locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalizationTest {

    @Test
    public void When_InitLocalization_Then_DefaultLanguageFullTranslated() {
        LocalizationInitializer.initLocale();
        final var coverage = LocalizationCoverage.coverage(Language.DEFAULT);
        Assertions.assertEquals(1, coverage);
    }
}
