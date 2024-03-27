package ru.homyakin.seeker.locale.common;

import java.util.HashMap;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.group.stats.GroupPersonageStats;
import ru.homyakin.seeker.telegram.group.stats.GroupStats;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class CommonLocalization {
    private static final Resources<CommonResource> resources = new Resources<>();

    public static void add(Language language, CommonResource resource) {
        resources.add(language, resource);
    }

    public static String welcomeGroup(Language language) {
        return resources.getOrDefault(language, CommonResource::welcomeGroup);
    }

    public static String welcomeUser(Language language) {
        return resources.getOrDefault(language, CommonResource::welcomeUser);
    }

    public static String chooseLanguage(Language language) {
        return resources.getOrDefault(language, CommonResource::chooseLanguage);
    }

    public static String onlyAdminLanguage(Language language) {
        return resources.getOrDefault(language, CommonResource::onlyAdminLanguage);
    }

    public static String internalError(Language language) {
        return resources.getOrDefault(language, CommonResource::internalError);
    }

    public static String fullProfile(Language language, Personage personage) {
        final var params = profileParams(personage);
        params.put("item_characteristics", ItemLocalization.characteristics(language, personage.itemCharacteristics()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::fullProfile),
            params
        );
    }

    public static String shortProfile(Language language, Personage personage) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::shortProfile),
            profileParams(personage)
        );
    }

    private static HashMap<String, Object> profileParams(Personage personage) {
        final var characteristics = personage.calcTotalCharacteristics();
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("energy_icon", Icons.ENERGY);
        params.put("personage_badge_with_name", personage.badgeWithName());
        params.put("personage_money", personage.money().value());
        params.put("energy_value", personage.energy().value());
        params.put("attack_icon", Icons.ATTACK);
        params.put("attack_value", characteristics.attack());
        params.put("defense_icon", Icons.DEFENSE);
        params.put("defense_value", characteristics.defense());
        params.put("strength_icon", Icons.STRENGTH);
        params.put("strength_value", characteristics.strength());
        params.put("agility_icon", Icons.AGILITY);
        params.put("agility_value", characteristics.agility());
        params.put("wisdom_icon", Icons.WISDOM);
        params.put("wisdom_value", characteristics.wisdom());
        params.put("health_icon", Icons.HEALTH);
        params.put("health_value", characteristics.health());
        return params;
    }

    public static String receptionDesk(Language language) {
        return resources.getOrDefault(language, CommonResource::receptionDesk);
    }

    public static String mainMenu(Language language) {
        return resources.getOrDefaultRandom(language, CommonResource::mainMenu);
    }

    public static String groupStats(Language language, GroupStats groupStats) {
        final var params = new HashMap<String, Object>();
        params.put("raids_count", groupStats.raidsComplete());
        params.put("duels_count", groupStats.duelsComplete());
        params.put("money_icon", Icons.MONEY);
        params.put("tavern_money_spent", groupStats.tavernMoneySpent());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::groupStats),
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
            resources.getOrDefault(language, CommonResource::personageGroupStats),
            params
        );
    }
}
