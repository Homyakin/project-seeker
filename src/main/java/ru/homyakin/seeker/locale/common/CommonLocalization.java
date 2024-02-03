package ru.homyakin.seeker.locale.common;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.stats.GroupPersonageStats;
import ru.homyakin.seeker.telegram.group.stats.GroupStats;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class CommonLocalization {
    private static final Map<Language, CommonResource> map = new HashMap<>();

    public static void add(Language language, CommonResource resource) {
        map.put(language, resource);
    }

    public static String welcomeGroup(Language language) {
        return CommonUtils.ifNullThen(map.get(language).welcomeGroup(), map.get(Language.DEFAULT).welcomeGroup());
    }

    public static String welcomeUser(Language language) {
        return CommonUtils.ifNullThen(map.get(language).welcomeUser(), map.get(Language.DEFAULT).welcomeUser());
    }

    public static String chooseLanguage(Language language) {
        return CommonUtils.ifNullThen(map.get(language).chooseLanguage(), map.get(Language.DEFAULT).chooseLanguage());
    }

    public static String onlyAdminLanguage(Language language) {
        return CommonUtils.ifNullThen(map.get(language).onlyAdminLanguage(), map.get(Language.DEFAULT).onlyAdminLanguage());
    }

    public static String internalError(Language language) {
        return CommonUtils.ifNullThen(map.get(language).internalError(), map.get(Language.DEFAULT).internalError());
    }

    public static String fullProfile(Language language, Personage personage) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).fullProfile(), map.get(Language.DEFAULT).fullProfile()),
            profileParams(personage)
        );
    }

    public static String shortProfile(Language language, Personage personage) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).shortProfile(), map.get(Language.DEFAULT).shortProfile()),
            profileParams(personage)
        );
    }

    private static HashMap<String, Object> profileParams(Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("energy_icon", Icons.ENERGY);
        params.put("personage_badge_with_name", personage.iconWithName());
        params.put("personage_money", personage.money().value());
        params.put("energy_value", personage.energy().value());
        params.put("attack_icon", Icons.ATTACK);
        params.put("attack_value", personage.characteristics().attack());
        params.put("defense_icon", Icons.DEFENSE);
        params.put("defense_value", personage.characteristics().defense());
        params.put("strength_icon", Icons.STRENGTH);
        params.put("strength_value", personage.characteristics().strength());
        params.put("agility_icon", Icons.AGILITY);
        params.put("agility_value", personage.characteristics().agility());
        params.put("wisdom_icon", Icons.WISDOM);
        params.put("wisdom_value", personage.characteristics().wisdom());
        params.put("health_icon", Icons.HEALTH);
        params.put("health_value", personage.characteristics().health());
        return params;
    }

    public static String receptionDesk(Language language) {
        return CommonUtils.ifNullThen(map.get(language).receptionDesk(), map.get(Language.DEFAULT).receptionDesk());
    }

    public static String mainMenu(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThen(map.get(language).mainMenu(), map.get(Language.DEFAULT).mainMenu())
        );
    }

    public static String groupStats(Language language, GroupStats groupStats) {
        final var params = new HashMap<String, Object>();
        params.put("raids_count", groupStats.raidsComplete());
        params.put("duels_count", groupStats.duelsComplete());
        params.put("money_icon", Icons.MONEY);
        params.put("tavern_money_spent", groupStats.tavernMoneySpent());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).groupStats(), map.get(Language.DEFAULT).groupStats()),
            params
        );
    }

    public static String personageGroupStats(Language language, GroupPersonageStats stats) {
        final var params = new HashMap<String, Object>();
        params.put("raids_success", stats.raidsSuccess());
        params.put("raids_total", stats.raidsTotal());
        params.put("duels_wins", stats.duelsWins());
        params.put("duels_total", stats.duelsTotal());
        params.put("money_icon", Icons.MONEY);
        params.put("tavern_money_spent", stats.tavernMoneySpent());
        params.put("spin_wins_count", stats.spinWinsCount());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).personageGroupStats(), map.get(Language.DEFAULT).personageGroupStats()),
            params
        );
    }
}
