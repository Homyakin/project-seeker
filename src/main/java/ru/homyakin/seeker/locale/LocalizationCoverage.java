package ru.homyakin.seeker.locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import ru.homyakin.seeker.infrastructure.init.Events;
import ru.homyakin.seeker.infrastructure.init.Items;
import ru.homyakin.seeker.infrastructure.init.Rumors;
import ru.homyakin.seeker.utils.ResourceUtils;

public class LocalizationCoverage {
    private static final HashMap<TranslateType, TranslatedData> translateInfo = new HashMap<>();
    private static final HashMap<Language, Double> translateCoverage = new HashMap<>();

    public static double coverage(Language language) {
        return translateCoverage.computeIfAbsent(
            language,
            LocalizationCoverage::countCoverage
        );
    }

    public static void fillLocalizationData(ObjectMapper mapper, String localizationFolder) {
        final var locales = new HashMap<Language, Map<String, String>>();

        for (final var language : Language.values()) {
            // Это плоская мапа, чтобы было удобно делать подсчёт перевода
            final var languageMap = new HashMap<String, String>();
            ResourceUtils.listAllFiles(localizationFolder + language.value())
                .forEach(path -> {
                    try (final var stream = Files.newInputStream(path)) {
                        final var map = mapper.readValue(stream, HashMap.class);
                        fillMap(languageMap, map, path.getFileName().toString());
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            locales.put(language, languageMap);
        }

        //считаем все заполненные ключи и количество переведённых ключей в каждой локализации
        final var allKeys = new HashSet<String>();
        final var translatedKeys = new HashMap<Language, Integer>();
        for (final var locale : locales.entrySet()) {
            allKeys.addAll(locale.getValue().keySet());
            translatedKeys.put(locale.getKey(), locale.getValue().size());
        }

        translateInfo.put(
            TranslateType.COMMON,
            new TranslatedData(allKeys.size(), translatedKeys)
        );
    }

    public static void addEventsInfo(Events events) {
        final var count = events.event().size();
        final var translatedEvents = new HashMap<Language, Integer>();
        events.event().forEach(
            event -> event.locales().forEach(
                locale -> translatedEvents.merge(locale.language(), 1, Integer::sum)
            )
        );
        translateInfo.put(
            TranslateType.EVENTS,
            new TranslatedData(
                count,
                translatedEvents
            )
        );
    }

    public static void addRumorsInfo(Rumors rumors) {
        final var count = rumors.rumor().size();
        final var translatedEvents = new HashMap<Language, Integer>();
        rumors.rumor().forEach(
            rumor -> rumor.locales().forEach(
                locale -> translatedEvents.merge(locale.language(), 1, Integer::sum)
            )
        );
        translateInfo.put(
            TranslateType.RUMORS,
            new TranslatedData(
                count,
                translatedEvents
            )
        );
    }

    public static void addMenuItemsInfo(Items items) {
        final var count = items.item().size();
        final var translatedEvents = new HashMap<Language, Integer>();
        items.item().forEach(
            item -> item.locales().forEach(
                locale -> translatedEvents.merge(locale.language(), 1, Integer::sum)
            )
        );
        translateInfo.put(
            TranslateType.MENU_ITEMS,
            new TranslatedData(
                count,
                translatedEvents
            )
        );
    }

    private static void fillMap(Map<String, String> targetMap, Map<String, Object> sourceMap, String keyPrefix) {
        for (final var entry : sourceMap.entrySet()) {
            if (entry.getValue() instanceof Map map) {
                fillMap(targetMap, map, keyPrefix + entry.getKey());
            } else {
                targetMap.put(keyPrefix + entry.getKey(), entry.getValue().toString());
            }
        }
    }

    private static double countCoverage(Language language) {
        var totalCount = 0;
        var translatedRows = 0;
        for (final var data: translateInfo.values()) {
            totalCount += data.rowCount();
            translatedRows += data.translatedRows.getOrDefault(language, 0);
        }
        return (double) translatedRows / totalCount;
    }

    private record TranslatedData(
        int rowCount,
        Map<Language, Integer> translatedRows
    ) {
    }

    private enum TranslateType {
        COMMON,
        EVENTS,
        RUMORS,
        MENU_ITEMS,
    }
}
