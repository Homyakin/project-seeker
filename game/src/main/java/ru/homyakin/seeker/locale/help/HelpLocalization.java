package ru.homyakin.seeker.locale.help;

import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.ActiveSkillSlots;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
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
        params.put("energy_icon", Icons.ENERGY);
        params.put("settings_command", CommandType.SETTINGS.getText());
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
        params.put("throw_command", CommandType.THROW_ORDER.getText());
        params.put("throw_to_group_command", CommandType.THROW_ORDER_TO_GROUP.getText());
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
        params.put("normal_attack_icon", Icons.NORMAL_ATTACK);
        params.put("dodge_icon", Icons.DODGE);
        params.put("crit_attack_icon", Icons.CRIT_ATTACK);
        params.put("speed_icon", Icons.SPEED);
        params.put("range_icon", Icons.RANGE);
        params.put("threat_icon", Icons.THREAT);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::battleSystem),
            params
        );
    }

    public static String battleMatrix(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("slash_icon", Icons.ATTACK_TYPE_SLASH);
        params.put("blunt_icon", Icons.ATTACK_TYPE_BLUNT);
        params.put("pierce_icon", Icons.ATTACK_TYPE_PIERCE);
        params.put("magical_icon", Icons.ATTACK_TYPE_MAGICAL);
        params.put("cloth_icon", Icons.DEFENSE_TYPE_CLOTH);
        params.put("leather_icon", Icons.DEFENSE_TYPE_LEATHER);
        params.put("plate_icon", Icons.DEFENSE_TYPE_PLATE);
        params.put("arcane_icon", Icons.DEFENSE_TYPE_ARCANE);
        params.put("matrix_table", damageMatrixTable());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::battleMatrix),
            params
        );
    }

    private static String damageMatrixTable() {
        final var attacks = AttackType.values();
        final var defenses = DefenseType.values();
        final var sb = new StringBuilder();
        sb.append("  ");
        for (final var attack : attacks) {
            sb.append(' ').append(Icons.attackTypeIcon(attack));
        }
        sb.append('\n');
        for (final var defense : defenses) {
            sb.append(Icons.defenseTypeIcon(defense));
            for (final var attack : attacks) {
                final var percent = BattlePersonage.damageMitigationPercent(defense, attack);
                sb.append(String.format(" %3d%%", percent));
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    public static String battleSkill(Language language, ActiveEnum activeEnum) {
        final var slots = ActiveSkillSlots.slotsFor(activeEnum).stream()
            .sorted((a, b) -> Integer.compare(a.id, b.id))
            .map(slot -> slot.icon)
            .collect(Collectors.joining());
        final var params = new HashMap<String, Object>();
        params.put("skill_name", BattleLocalization.skillName(language, activeEnum));
        params.put("slots", slots.isEmpty() ? "—" : slots);
        params.put("description", BattleLocalization.skillGeneralDescription(language, activeEnum));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::battleSkill),
            params
        );
    }

    public static String battleSkillsEmpty(Language language) {
        return resources.getOrDefault(language, HelpResource::battleSkillsEmpty);
    }

    public static String seasons(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, HelpResource::seasons),
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

    public static String battleGeneralButton(Language language) {
        return resources.getOrDefault(language, HelpResource::battleGeneralButton);
    }

    public static String battleMatrixButton(Language language) {
        return resources.getOrDefault(language, HelpResource::battleMatrixButton);
    }

    public static String battleSkillsButton(Language language) {
        return resources.getOrDefault(language, HelpResource::battleSkillsButton);
    }

    public static String battleSkillsAllFilterButton(Language language) {
        return resources.getOrDefault(language, HelpResource::battleSkillsAllFilterButton);
    }

    public static String battleSkillsPrevButton(Language language) {
        return resources.getOrDefault(language, HelpResource::battleSkillsPrevButton);
    }

    public static String battleSkillsNextButton(Language language) {
        return resources.getOrDefault(language, HelpResource::battleSkillsNextButton);
    }

    public static String seasonsButton(Language language) {
        return resources.getOrDefault(language, HelpResource::seasonsButton);
    }
}
