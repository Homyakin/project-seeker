package ru.homyakin.seeker.locale.help;

import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.TelegramBotConfig;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class HelpLocalization {
    private static final Resources<HelpResource> resources = new Resources<>();

    public static void add(Language language, HelpResource resource) {
        resources.add(language, resource);
    }

    public static String main(Language language) {
        return resources.getOrDefault(language, HelpResource::main);
    }

    public static String raids(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("set_active_time_command", CommandType.SET_ACTIVE_TIME.getText());
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::raids),
            params
        );
    }

    public static String duels(Language language) {
        final var param = Collections.<String, Object>singletonMap("duel_command", CommandType.START_DUEL.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::duels),
            param
        );
    }

    public static String menu(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("menu_command", CommandType.TAVERN_MENU.getText());
        params.put("order_command", CommandType.ORDER.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::menu),
            params
        );
    }

    public static String personage(Language language) {
        final var param = Collections.<String, Object>singletonMap("bot_username", "@" + TelegramBotConfig.username());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::personage),
            param
        );
    }

    public static String info(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
        params.put("source_code_link", TextConstants.SOURCE_LINK);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::info),
            params
        );
    }

    public static String battleSystem(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("health_icon", Icons.HEALTH);
        params.put("attack_icon", Icons.ATTACK);
        params.put("defense_icon", Icons.DEFENSE);
        params.put("strength_icon", Icons.STRENGTH);
        params.put("agility_icon", Icons.AGILITY);
        params.put("wisdom_icon", Icons.WISDOM);
        params.put("normal_attack_icon", Icons.NORMAL_ATTACK);
        params.put("dodge_icon", Icons.DODGE);
        params.put("crit_attack_icon", Icons.CRIT_ATTACK);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::battleSystem),
            params
        );
    }

    public static String raidsButton(Language language) {
        return resources.getOrDefault(language, HelpResource::raidsButton);
    }

    public static String duelsButton(Language language) {
        return resources.getOrDefault(language, HelpResource::duelsButton);
    }

    public static String menuButton(Language language) {
        return resources.getOrDefault(language, HelpResource::menuButton);
    }

    public static String personageButton(Language language) {
        return resources.getOrDefault(language, HelpResource::personageButton);
    }

    public static String infoButton(Language language) {
        return resources.getOrDefault(language, HelpResource::infoButton);
    }

    public static String battleSystemButton(Language language) {
        return resources.getOrDefault(language, HelpResource::battleSystemButton);
    }
}
