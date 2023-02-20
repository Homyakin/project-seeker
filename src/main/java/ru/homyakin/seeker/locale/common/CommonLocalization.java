package ru.homyakin.seeker.locale.common;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class CommonLocalization {
    private static final Map<Language, CommonResource> map = new HashMap<>();

    public static void add(Language language, CommonResource resource) {
        map.put(language, resource);
    }

    public static String welcomeGroup(Language language) {
        return CommonUtils.ifNullThan(map.get(language).welcomeGroup(), map.get(Language.DEFAULT).welcomeGroup());
    }

    public static String welcomeUser(Language language) {
        return CommonUtils.ifNullThan(map.get(language).welcomeUser(), map.get(Language.DEFAULT).welcomeUser());
    }

    public static String chooseLanguage(Language language) {
        return CommonUtils.ifNullThan(map.get(language).chooseLanguage(), map.get(Language.DEFAULT).chooseLanguage());
    }

    public static String onlyAdminLanguage(Language language) {
        return CommonUtils.ifNullThan(map.get(language).onlyAdminLanguage(), map.get(Language.DEFAULT).onlyAdminLanguage());
    }

    public static String internalError(Language language) {
        return CommonUtils.ifNullThan(map.get(language).internalError(), map.get(Language.DEFAULT).internalError());
    }

    public static String help(Language language) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).help(), map.get(Language.DEFAULT).help()),
            new HashMap<>() {{
                put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
                put("language_command", CommandType.CHANGE_LANGUAGE.getText());
                put("profile_command", CommandType.GET_PROFILE.getText());
                put("help_command", CommandType.HELP.getText());
                put("duel_command", CommandType.START_DUEL.getText());
                put("name_command", CommandType.CHANGE_NAME.getText());
                put("github_link", TextConstants.SOURCE_LINK);
            }}
        );
    }

    public static String profileTemplate(Language language, Personage personage) {
        final var params = new HashMap<String, Object>() {{
            put("personage_icon", TextConstants.PERSONAGE_ICON);
            put("money_icon", TextConstants.MONEY_ICON);
            put("personage_name", personage.name());
            put("personage_money", personage.money().value());
        }};
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).profileTemplate(), map.get(Language.DEFAULT).profileTemplate()),
            params
        );
    }
}
