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
import ru.homyakin.seeker.locale.feedback.FeedbackLocalization;
import ru.homyakin.seeker.locale.feedback.FeedbackResource;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsLocalization;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsResource;
import ru.homyakin.seeker.locale.help.HelpLocalization;
import ru.homyakin.seeker.locale.help.HelpResource;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.locale.item.ItemResource;
import ru.homyakin.seeker.locale.personal.BadgeLocalization;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.locale.personal.MenuLocalization;
import ru.homyakin.seeker.locale.personal.PersonalQuestLocalization;
import ru.homyakin.seeker.locale.personal.PersonalResource;
import ru.homyakin.seeker.locale.personal.SettingsLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.locale.raid.RaidResource;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.locale.shop.ShopResource;
import ru.homyakin.seeker.locale.spin.EverydaySpinLocalization;
import ru.homyakin.seeker.locale.spin.EverydaySpinResource;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuResource;
import ru.homyakin.seeker.locale.top.TopLocalization;
import ru.homyakin.seeker.locale.top.TopResource;
import ru.homyakin.seeker.telegram.command.type.ChangeNameCommandType;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.command.type.FeedbackCommandType;
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
    private static final String TOP_PATH = File.separator + "top.toml";
    private static final String ITEM_PATH = File.separator + "item.toml";
    private static final String SHOP_PATH = File.separator + "shop.toml";
    private static final String FEEDBACK_PATH = File.separator + "feedback.toml";
    private static final Logger logger = LoggerFactory.getLogger(LocalizationInitializer.class);

    public static void initLocale() {
        logger.info("Filling localization");
        final var mapper = TomlMapper.builder().build();
        final var languages = Language.values();
        LocalizationCoverage.fillLocalizationData(mapper, LOCALIZATION_PATH);

        for (final var language : languages) {
            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + COMMON_PATH,
                it -> CommonLocalization.add(language, extractClass(mapper, it, CommonResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + DUEL_PATH,
                it -> DuelLocalization.add(language, extractClass(mapper, it, DuelResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + PERSONAL_PATH,
                it -> {
                    final var resource = extractClass(mapper, it, PersonalResource.class);
                    ChangeNameLocalization.add(language, resource.changeName());
                    CharacteristicLocalization.add(language, resource.characteristics());
                    MenuLocalization.add(language, resource.menu());
                    BadgeLocalization.add(language, resource.badges());
                    PersonalQuestLocalization.add(language, resource.personalQuest());
                    SettingsLocalization.add(language, resource.settings());
                    CommandType.fillLocaleMap(resource.menu());
                    ChangeNameCommandType.fillLocaleMap(resource.changeName());
                }
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + RAID_PATH,
                it -> RaidLocalization.add(language, extractClass(mapper, it, RaidResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + TAVERN_MENU_PATH,
                it -> TavernMenuLocalization.add(language, extractClass(mapper, it, TavernMenuResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + HELP_PATH,
                it -> HelpLocalization.add(language, extractClass(mapper, it, HelpResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + EVERYDAY_SPIN_PATH,
                it -> EverydaySpinLocalization.add(language, extractClass(mapper, it, EverydaySpinResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + GROUP_SETTINGS_PATH,
                it -> GroupSettingsLocalization.add(language, extractClass(mapper, it, GroupSettingsResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + TOP_PATH,
                it -> TopLocalization.add(language, extractClass(mapper, it, TopResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + ITEM_PATH,
                it -> ItemLocalization.add(language, extractClass(mapper, it, ItemResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + SHOP_PATH,
                it -> ShopLocalization.add(language, extractClass(mapper, it, ShopResource.class))
            );

            ResourceUtils.doAction(
                LOCALIZATION_PATH + language.value() + FEEDBACK_PATH,
                it -> {
                    final var resource = extractClass(mapper, it, FeedbackResource.class);
                    FeedbackLocalization.add(language, resource);
                    FeedbackCommandType.fillLocaleMap(resource);
                }
            );
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
