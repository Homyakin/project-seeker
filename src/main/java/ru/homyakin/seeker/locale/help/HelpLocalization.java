package ru.homyakin.seeker.locale.help;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.TelegramBotConfig;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class HelpLocalization {
    private static final Map<Language, HelpResource> map = new HashMap<>();

    public static void add(Language language, HelpResource resource) {
        map.put(language, resource);
    }

    public static String main(Language language) {
        return CommonUtils.ifNullThen(map.get(language).main(), map.get(Language.DEFAULT).main());
    }

    public static String raids(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("set_active_time_command", CommandType.SET_ACTIVE_TIME.getText());
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).raids(), map.get(Language.DEFAULT).raids()),
            params
        );
    }

    public static String duels(Language language) {
        final Map<String, Object> param = Collections.singletonMap("duel_command", CommandType.START_DUEL.getText());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).duels(), map.get(Language.DEFAULT).duels()),
            param
        );
    }

    public static String menu(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("menu_command", CommandType.TAVERN_MENU.getText());
        params.put("order_command", CommandType.ORDER.getText());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).menu(), map.get(Language.DEFAULT).menu()),
            params
        );
    }

    public static String personage(Language language) {
        final Map<String, Object> param = Collections.singletonMap("bot_username", "@" + TelegramBotConfig.username());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).personage(), map.get(Language.DEFAULT).personage()),
            param
        );
    }

    public static String info(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
        params.put("source_code_link", TextConstants.SOURCE_LINK);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).info(), map.get(Language.DEFAULT).info()),
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
            CommonUtils.ifNullThen(map.get(language).battleSystem(), map.get(Language.DEFAULT).battleSystem()),
            params
        );
    }

    public static String raidsButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).raidsButton(), map.get(Language.DEFAULT).raidsButton());
    }

    public static String duelsButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).duelsButton(), map.get(Language.DEFAULT).duelsButton());
    }

    public static String menuButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).menuButton(), map.get(Language.DEFAULT).menuButton());
    }

    public static String personageButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).personageButton(), map.get(Language.DEFAULT).personageButton());
    }

    public static String infoButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).infoButton(), map.get(Language.DEFAULT).infoButton());
    }

    public static String battleSystemButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).battleSystemButton(), map.get(Language.DEFAULT).battleSystemButton());
    }
}
