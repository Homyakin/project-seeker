package ru.homyakin.seeker.locale.common;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.group.models.GroupStats;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
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

    public static String profileTemplate(Language language, Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("personage_icon", TextConstants.PERSONAGE_ICON);
        params.put("money_icon", TextConstants.MONEY_ICON);
        params.put("personage_name", personage.name());
        params.put("personage_money", personage.money().value());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).profileTemplate(), map.get(Language.DEFAULT).profileTemplate()),
            params
        );
    }

    public static String receptionDesk(Language language) {
        return CommonUtils.ifNullThan(map.get(language).receptionDesk(), map.get(Language.DEFAULT).receptionDesk());
    }

    public static String mainMenu(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).mainMenu(), map.get(Language.DEFAULT).mainMenu())
        );
    }

    public static String groupStats(Language language, GroupStats groupStats) {
        final var params = new HashMap<String, Object>();
        params.put("raids_count", groupStats.raidsComplete());
        params.put("duels_count", groupStats.duelsComplete());
        params.put("money_icon", TextConstants.MONEY_ICON);
        params.put("tavern_money_spent", groupStats.tavernMoneySpent());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).groupStats(), map.get(Language.DEFAULT).groupStats()),
            params
        );
    }
}
