package ru.homyakin.seeker.locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import ru.homyakin.seeker.infrastructure.init.saving_models.Badges;
import ru.homyakin.seeker.infrastructure.init.saving_models.PersonalQuests;
import ru.homyakin.seeker.infrastructure.init.saving_models.Raids;
import ru.homyakin.seeker.infrastructure.init.saving_models.WorldRaids;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemModifiers;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemObjects;
import ru.homyakin.seeker.infrastructure.init.saving_models.Items;
import ru.homyakin.seeker.infrastructure.init.saving_models.Rumors;
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

    public static void addRaidsInfo(Raids raids) {
        addLocalizedInfo(raids.raid(), TranslateType.RAIDS);
    }

    public static void addPersonalQuestsInfo(PersonalQuests personalQuests) {
        addLocalizedInfo(personalQuests.quest(), TranslateType.PERSONAL_QUESTS);
    }

    public static void addRumorsInfo(Rumors rumors) {
        addLocalizedInfo(rumors.rumor(), TranslateType.RUMORS);
    }

    public static void addBadgesInfo(Badges badges) {
        addLocalizedInfo(badges.badge(), TranslateType.BADGES);
    }

    public static void addItemObjectsInfo(ItemObjects itemObjects) {
        addLocalizedInfo(itemObjects.object(), TranslateType.ITEM_OBJECTS);
    }

    public static void addItemModifiersInfo(ItemModifiers itemModifiers) {
        addLocalizedInfo(itemModifiers.modifier(), TranslateType.ITEM_MODIFIERS);
    }

    public static void addWorldRaidsInfo(WorldRaids worldRaids) {
        addLocalizedInfo(worldRaids.raid(), TranslateType.WORLD_RAIDS);
    }

    public static void addMenuItemsInfo(Items items) {
        addLocalizedInfo(items.item(), TranslateType.MENU_ITEMS);
    }

    private static <T extends Localized<?>> void addLocalizedInfo(List<T> locales, TranslateType type) {
        final var count = locales.size(); // Считаем общее количество переведённых объектов
        final var translatedLocales = new HashMap<Language, Integer>();
        locales.forEach(
            // Считаем по каждому объекту на какие языки он был переведён и добавляем к общей сумме языка
            localized -> localized.locales().forEach(
                (language, _) -> translatedLocales.merge(language, 1, Integer::sum)
            )
        );
        translateInfo.put(
            type,
            new TranslatedData(
                count,
                translatedLocales
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
        RAIDS,
        PERSONAL_QUESTS,
        RUMORS,
        MENU_ITEMS,
        BADGES,
        ITEM_OBJECTS,
        ITEM_MODIFIERS,
        WORLD_RAIDS,
    }
}
