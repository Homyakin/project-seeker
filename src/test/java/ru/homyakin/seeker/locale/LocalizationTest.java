package ru.homyakin.seeker.locale;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalizationTest {
    final List<String> keys = Arrays.stream(LocalizationKeys.values()).map(Enum::name).toList();

}
