package ru.homyakin.seeker.locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.File;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.common.CommonResource;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.locale.duel.DuelResource;
import ru.homyakin.seeker.locale.group_settings.ActiveTimeLocalization;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsResource;
import ru.homyakin.seeker.locale.help.HelpLocalization;
import ru.homyakin.seeker.locale.help.HelpResource;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.locale.personal.MenuLocalization;
import ru.homyakin.seeker.locale.personal.PersonalResource;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.locale.raid.RaidResource;
import ru.homyakin.seeker.locale.spin.EverydaySpinLocalization;
import ru.homyakin.seeker.locale.spin.EverydaySpinResource;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuResource;
import ru.homyakin.seeker.telegram.command.type.ChangeNameCommandType;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.ResourceUtils;

public class LocalizationInitializer {
    private static final String LOCALIZATION_PATH = "localization" + File.separator;

    private static final String COMMON_PATH = File.separator + "common.toml";
    private static final String DUEL_PATH = File.separator + "duel.toml";
    private static final String PERSONAL_PATH = File.separator + "personal.toml";
    private static final String RAID_PATH = File.separator + "raid.toml";
    private static final String TAVERN_MENU_PATH = File.separator + "tavern_menu.toml";
    private static final String HELP_PATH = File.separator + "help.toml";
    private static final String EVERYDAY_SPIN_PATH = File.separator + "everyday_spin.toml";
    private static final String GROUP_SETTINGS_PATH = File.separator + "group_settings.toml";
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
                    CharacteristicLocalization.add(language, resource.characteristics());
                    MenuLocalization.add(language, resource.menu());
                    CommandType.fillLocaleMap(resource.menu());
                    ChangeNameCommandType.fillLocaleMap(resource.changeName());
                });

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + RAID_PATH)
                .ifPresent(it -> RaidLocalization.add(language, extractClass(mapper, it, RaidResource.class)));

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + TAVERN_MENU_PATH)
                .ifPresent(it -> TavernMenuLocalization.add(language, extractClass(mapper, it, TavernMenuResource.class)));

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + HELP_PATH)
                .ifPresent(it -> HelpLocalization.add(language, extractClass(mapper, it, HelpResource.class)));

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + EVERYDAY_SPIN_PATH)
                .ifPresent(it -> EverydaySpinLocalization.add(language, extractClass(mapper, it, EverydaySpinResource.class)));

            ResourceUtils.getResourcePath(LOCALIZATION_PATH + language.value() + GROUP_SETTINGS_PATH)
                .ifPresent(it -> {
                    final var resource = extractClass(mapper, it, GroupSettingsResource.class);
                    ActiveTimeLocalization.add(language, resource.activeTime());
                });
        }
        logger.info("Localization loaded");
    }

    private static <T> T extractClass(ObjectMapper mapper, InputStream stream, Class<T> clazz) {
        try {
            return mapper.readValue(stream, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Can't parse locale for " + clazz.getSimpleName(), e);
        }
    }
}
