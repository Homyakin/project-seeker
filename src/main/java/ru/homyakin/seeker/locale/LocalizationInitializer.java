package ru.homyakin.seeker.locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.common.CommonResource;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.locale.duel.DuelResource;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.locale.personal.LevelingLocalization;
import ru.homyakin.seeker.locale.personal.MenuLocalization;
import ru.homyakin.seeker.locale.personal.MenuResource;
import ru.homyakin.seeker.locale.personal.PersonalResource;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.locale.raid.RaidResource;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuResource;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.ResourceUtils;

public class LocalizationInitializer {
    private static final String LOCALIZATION_PATH = "localization/";

    private static final String COMMON_PATH = "/common.toml";
    private static final String DUEL_PATH = "/duel.toml";
    private static final String PERSONAL_PATH = "/personal.toml";
    private static final String RAID_PATH = "/raid.toml";
    private static final String TAVERN_MENU_PATH = "/tavern_menu.toml";
    private static final Logger logger = LoggerFactory.getLogger(LocalizationInitializer.class);

    public static void initLocale() {
        logger.info("Filling localization");
        final var mapper = TomlMapper.builder().build();
        final var languages = Language.values();

        for (final var language : languages) {
            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + COMMON_PATH)
                .ifPresent(it -> CommonLocalization.add(language, extractClass(mapper, it, CommonResource.class)));

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + DUEL_PATH)
                .ifPresent(it -> DuelLocalization.add(language, extractClass(mapper, it, DuelResource.class)));

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + PERSONAL_PATH)
                .ifPresent(it -> {
                    final var resource = extractClass(mapper, it, PersonalResource.class);
                    ChangeNameLocalization.add(language, resource.changeName());
                    LevelingLocalization.add(language, resource.leveling());
                    MenuLocalization.add(language, resource.menu());
                    CommandType.fillLocaleMap(resource.menu());
                });

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + RAID_PATH)
                .ifPresent(it -> RaidLocalization.add(language, extractClass(mapper, it, RaidResource.class)));

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + TAVERN_MENU_PATH)
                .ifPresent(it -> TavernMenuLocalization.add(language, extractClass(mapper, it, TavernMenuResource.class)));
        }
        logger.info("Localization loaded");
    }

    private static <T> T extractClass(ObjectMapper mapper, Path path, Class<T> clazz) {
        try {
            return mapper.readValue(path.toFile(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Can't parse locale " + path.toString(), e);
        }
    }
}
