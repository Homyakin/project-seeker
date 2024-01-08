package ru.homyakin.seeker.locale;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.utils.ResourceUtils;

public class LocalizationTest {
    private static final String LOCALIZATION_PATH = "localization/%s/%s.toml";
    private static final List<String> FILES = List.of("common", "duel", "personal", "raid");

    @Test
    @DisplayName("Default translate contains all keys")
    public void defaultFullTranslate() throws IOException {
        final var mapper = TomlMapper.builder().build();
        final var locales = new HashMap<Language, Map<String, String>>();

        //считываем все переводы в мапу
        for (final var language: Language.values()) {
            final var languageMap = new HashMap<String, String>();
            for (final var file : FILES) {
                final var stream = ResourceUtils.doAction(LOCALIZATION_PATH.formatted(language.value(), file)).get();
                final Map<String, Object> map = mapper.readValue(stream, HashMap.class);
                fillMap(languageMap, map, file);
            }
            locales.put(language, languageMap);
        }

        //считаем все заполненные ключи
        final var allKeys = new HashSet<String>();
        for (final var locale: locales.entrySet()) {
            allKeys.addAll(locale.getValue().keySet());
        }

        // Смотрим процент перевода
        final var allKeySize = allKeys.size();
        for (final var locale: locales.entrySet()) {
            final var localeKeySize = locale.getValue().keySet().size();
            if (locale.getKey() == Language.DEFAULT) {
                Assertions.assertEquals(allKeySize, localeKeySize);
            }
            System.out.printf(
                "Language %s translated %.2f%%%n",
                locale.getKey().value(),
                ((double) localeKeySize) / allKeySize * 100
            );
        }
    }

    private void fillMap(Map<String, String> targetMap, Map<String, Object> sourceMap, String keyPrefix) {
        for (final var entry : sourceMap.entrySet()) {
            if (entry.getValue() instanceof Map map) {
                fillMap(targetMap, map, keyPrefix + entry.getKey());
            } else {
                targetMap.put(keyPrefix + entry.getKey(), entry.getValue().toString());
            }
        }
    }
}
