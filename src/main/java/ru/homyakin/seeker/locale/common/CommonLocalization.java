package ru.homyakin.seeker.locale.common;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.event.launched.CurrentEvents;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.event.launched.CurrentEvent;
import ru.homyakin.seeker.game.group.entity.SavedGroupBattleResult;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageBattleResult;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.stats.entity.PersonageStats;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramBotConfig;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStats;
import ru.homyakin.seeker.game.stats.entity.GroupStats;
import ru.homyakin.seeker.telegram.statistic.Statistic;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommonLocalization {
    private static final Resources<CommonResource> resources = new Resources<>();

    public static void add(Language language, CommonResource resource) {
        resources.add(language, resource);
    }

    public static String welcomeGroup(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("settings_command", CommandType.SETTINGS.getText());
        params.put("menu_command", CommandType.TAVERN_MENU.getText());
        params.put("help_command", CommandType.SHOW_HELP.getText());
        params.put("duel_command", CommandType.START_DUEL.getText());
        params.put("spin_command", CommandType.SPIN.getText());
        params.put("top_command", CommandType.TOP.getText());
        params.put("bot_username", TelegramBotConfig.username());
        params.put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::welcomeGroup),
            params
        );
    }

    public static String welcomeUser(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("help_command", CommandType.SHOW_HELP.getText());
        params.put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::welcomeUser),
            params
        );
    }

    public static String chooseLanguage(Language language) {
        return resources.getOrDefault(language, CommonResource::chooseLanguage);
    }

    public static String onlyAdminAction(Language language) {
        return resources.getOrDefault(language, CommonResource::onlyAdminAction);
    }

    public static String onlyGroupMemberAction(Language language) {
        return resources.getOrDefault(language, CommonResource::onlyGroupMemberAction);
    }

    public static String forbiddenForHiddenGroup(Language language) {
        return resources.getOrDefault(language, CommonResource::forbiddenForHiddenGroup);
    }

    public static String onlyForRegisteredGroup(Language language) {
        return resources.getOrDefault(language, CommonResource::onlyForRegisteredGroup);
    }

    public static String forbiddenAction(Language language) {
        return resources.getOrDefault(language, CommonResource::forbiddenAction);
    }

    public static String internalError(Language language) {
        return resources.getOrDefault(language, CommonResource::internalError);
    }

    public static String fullProfile(Language language, Personage personage, CurrentEvents currentEvents) {
        final var params = profileParams(personage);

        params.put("item_characteristics", ItemLocalization.characteristics(language, personage.itemCharacteristics()));
        if (personage.energy().isFull()) {
            params.put("time_icon", "");
            params.put("remain_duration_for_full_regen", "");
        } else {
            params.put("time_icon", Icons.TIME);
            params.put(
                "remain_duration_for_full_regen",
                duration(language, personage.energy().remainTimeForFullRegen(TimeUtils.moscowTime()))
            );
        }
        if (personage.effects().isEmpty()) {
            params.put("personage_effects", "");
        } else {
            params.put("personage_effects", personageEffects(language, personage.effects()));
        }
        if (currentEvents.events().isEmpty()) {
            params.put("current_event", "");
        } else {
            params.put(
                "current_event",
                currentEvents
                    .events()
                    .stream()
                    .map(currentEvent -> personageInEvent(language, currentEvent))
                    .collect(Collectors.joining())
                    + "\n"
            );
        }
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
        final var characteristics = personage.calcTotalCharacteristicsWithEffects();
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("energy_icon", Icons.ENERGY);
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
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

    private static String personageEffects(Language language, PersonageEffects effects) {
        if (effects.isEmpty()) {
            return "";
        }
        final var effectsText = new StringBuilder();
        for (final var effect : effects.effects().entrySet()) {
            final var text = switch (effect.getKey()) {
                case MENU_ITEM_EFFECT -> menuItemEffect(language, effect.getValue());
                case THROW_DAMAGE_EFFECT -> throwOrderEffect(language, effect.getValue());
            };
            effectsText.append(text);
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageEffects),
            Collections.singletonMap("personage_effects", effectsText.toString())
        );
    }

    private static String menuItemEffect(Language language, PersonageEffect effect) {
        final var params = new HashMap<String, Object>();
        params.put("effect", effect(language, effect.effect()));
        params.put("time_icon", Icons.TIME);
        params.put("duration", duration(language, TimeUtils.moscowTime(), effect.expireDateTime()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::menuItemEffect),
            params
        );
    }

    private static String throwOrderEffect(Language language, PersonageEffect effect) {
        final var params = new HashMap<String, Object>();
        params.put("effect", effect(language, effect.effect()));
        params.put("time_icon", Icons.TIME);
        params.put("duration", duration(language, TimeUtils.moscowTime(), effect.expireDateTime()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::throwOrderEffect),
            params
        );
    }

    public static String duration(Language language, LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            return "0 " + minutesShort(language);
        }
        final var diff = Duration.between(start, end);
        return duration(language, diff);
    }

    public static String duration(Language language, Duration duration) {
        var hours = "";
        if (duration.toHours() > 0) {
            hours = duration.toHours() + " " + hoursShort(language);
        }
        var minutes = "";
        if (duration.toMinutesPart() > 0) {
            minutes = duration.toMinutesPart() + " " + minutesShort(language);
        } else if (duration.toHours() == 0) {
            minutes = "0 " + minutesShort(language);
        }
        if (hours.isBlank()) {
            return minutes;
        }
        return hours + " " + minutes;
    }

    private static String hoursShort(Language language) {
        return resources.getOrDefault(language, CommonResource::hoursShort);
    }

    private static String minutesShort(Language language) {
        return resources.getOrDefault(language, CommonResource::minutesShort);
    }

    public static String effect(Language language, Effect effect) {
        return switch (effect) {
            case Effect.Add add -> {
                final var params = new HashMap<String, Object>();
                params.put("value", add.value());
                params.put("characteristic_icon", add.characteristic().icon());
                yield StringNamedTemplate.format(
                    resources.getOrDefault(language, CommonResource::addValueEffect),
                    params
                );
            }
            case Effect.Multiplier multiplier -> {
                final var params = new HashMap<String, Object>();
                params.put("value", multiplier.percent());
                params.put("characteristic_icon", multiplier.characteristic().icon());
                yield StringNamedTemplate.format(
                    resources.getOrDefault(language, CommonResource::multiplyPercentEffect),
                    params
                );
            }
            case Effect.MinusMultiplier minusMultiplier -> {
                final var params = new HashMap<String, Object>();
                params.put("value", minusMultiplier.percent());
                params.put("characteristic_icon", minusMultiplier.characteristic().icon());
                yield StringNamedTemplate.format(
                    resources.getOrDefault(language, CommonResource::minusMultiplyPercentEffect),
                    params
                );
            }
        };
    }

    private static String personageInEvent(Language language, CurrentEvent event) {
        return switch (event.type()) {
            case RAID -> personageInRaid(language, event.endDate());
            case PERSONAL_QUEST -> personageInQuest(language, event.endDate());
            case WORLD_RAID -> personageInWorldRaid(language, event.endDate());
        };
    }

    private static String personageInRaid(Language language, LocalDateTime end) {
        final var params = new HashMap<String, Object>();
        params.put("time_icon", Icons.TIME);
        params.put("duration", CommonLocalization.duration(language, TimeUtils.moscowTime(), end));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageInRaid),
            params
        );
    }

    private static String personageInQuest(Language language, LocalDateTime end) {
        final var params = new HashMap<String, Object>();
        params.put("time_icon", Icons.TIME);
        params.put("duration", CommonLocalization.duration(language, TimeUtils.moscowTime(), end));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageInQuest),
            params
        );
    }

    private static String personageInWorldRaid(Language language, LocalDateTime end) {
        final var params = new HashMap<String, Object>();
        params.put("time_icon", Icons.TIME);
        params.put("duration", CommonLocalization.duration(language, TimeUtils.moscowTime(), end));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageInWorldRaid),
            params
        );
    }

    public static String receptionDesk(Language language, Statistic statistic) {
        final var params = new HashMap<String, Object>();
        params.put("active_personages_count", statistic.activePersonages());
        params.put("registered_groups_count", statistic.activeRegisteredGroups());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::receptionDesk),
            params
        );
    }

    public static String noStatsForSeason(Language language) {
        return resources.getOrDefault(language, CommonResource::noStatsForSeason);
    }

    public static String groupStats(Language language, GroupStats groupStats, Group group) {
        final var params = new HashMap<String, Object>();
        params.put("raids_success", groupStats.raidsSuccess());
        params.put("raids_total", groupStats.raidsTotal());
        params.put("duels_count", groupStats.duelsComplete());
        params.put("money_icon", Icons.MONEY);
        params.put("tavern_money_spent", groupStats.tavernMoneySpent());
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("world_raids_success", groupStats.worldRaidsSuccess());
        params.put("world_raids_total", groupStats.worldRaidsTotal());
        params.put("season_number", groupStats.seasonNumber().value());
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
        params.put("season_number", stats.seasonNumber().value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageGroupStats),
            params
        );
    }

    public static String migrateGroup(Language language) {
        return resources.getOrDefault(language, CommonResource::migrateGroup);
    }

    public static String energyRecovered(Language language) {
        return resources.getOrDefaultRandom(language, CommonResource::energyRecovered);
    }

    public static String personageGlobalStats(Language language, PersonageStats stats) {
        final var params = new HashMap<String, Object>();
        params.put("raids_success", stats.raidsSuccess());
        params.put("raids_total", stats.raidsTotal());
        params.put("duels_wins", stats.duelsWins());
        params.put("duels_total", stats.duelsTotal());
        params.put("money_icon", Icons.MONEY);
        params.put("tavern_money_spent", stats.tavernMoneySpent());
        params.put("spin_wins_count", stats.spinWinsCount());
        params.put("quests_success", stats.questsSuccess());
        params.put("quests_total", stats.questsTotal());
        params.put("world_raids_success", stats.worldRaidsSuccess());
        params.put("world_raids_total", stats.worldRaidsTotal());
        params.put("season_number", stats.seasonNumber().value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageGlobalStats),
            params
        );
    }

    public static String personageBattleReport(
        Language language,
        PersonageBattleResult result,
        LaunchedEvent event,
        Optional<Item> item
    ) {
        final var params = paramsForPersonageBattleReport(result);
        params.put("battle_date_time", TimeUtils.toString(event.endDate()));
        if (item.isEmpty()) {
            params.put("optional_full_item", "");
        } else {
            params.put("optional_full_item", ItemLocalization.fullItem(language, item.get()));
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageBattleReport),
            params
        );
    }

    public static String shortPersonageBattleReport(
        Language language,
        PersonageBattleResult result,
        Personage personage,
        Optional<Item> item
    ) {
        final var params = paramsForPersonageBattleReport(result);
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        if (item.isEmpty()) {
            params.put("optional_short_item_without_characteristics", "");
        } else {
            params.put(
                "optional_short_item_without_characteristics",
                ItemLocalization.shortItemWithoutCharacteristics(language, item.get())
            );
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::shortPersonageBattleReport),
            params
        );
    }

    private static Map<String, Object> paramsForPersonageBattleReport(PersonageBattleResult result) {
        final var params = new HashMap<String, Object>();
        params.put("attack_icon", Icons.ATTACK);
        params.put("attack_value", result.stats().characteristics().attack());
        params.put("defense_icon", Icons.DEFENSE);
        params.put("defense_value", result.stats().characteristics().defense());
        params.put("strength_icon", Icons.STRENGTH);
        params.put("strength_value", result.stats().characteristics().strength());
        params.put("agility_icon", Icons.AGILITY);
        params.put("agility_value", result.stats().characteristics().agility());
        params.put("wisdom_icon", Icons.WISDOM);
        params.put("wisdom_value", result.stats().characteristics().wisdom());
        params.put("normal_damage_value", result.stats().normalDamageDealt());
        params.put("normal_damage_count", result.stats().normalAttackCount());
        params.put("crit_damage_value", result.stats().critDamageDealt());
        params.put("crit_damage_count", result.stats().critsCount());
        params.put("misses_count", result.stats().missesCount());
        params.put("damage_blocked_value", result.stats().damageBlocked());
        params.put("damage_blocked_count", result.stats().blockCount());
        params.put("dodged_damage_value", result.stats().damageDodged());
        params.put("dodged_damage_count", result.stats().dodgesCount());
        params.put("remain_health", result.stats().remainHealth());
        params.put("max_health", result.stats().characteristics().health());
        params.put("money_icon", Icons.MONEY);
        params.put("reward_value", result.reward().value());
        params.put("normal_attack_icon", Icons.NORMAL_ATTACK);
        params.put("crit_attack_icon", Icons.CRIT_ATTACK);
        params.put("miss_icon", Icons.MISS);
        params.put("damage_blocked_icon", Icons.BLOCKED_DAMAGE);
        params.put("dodge_icon", Icons.DODGE);
        params.put("health_icon", Icons.HEALTH);
        return params;
    }

    public static String shortGroupBattleReport(Language language, SavedGroupBattleResult result, Group group) {
        final var params = new HashMap<String, Object>();
        params.put("group_badge_with_name", LocaleUtils.groupNameWithBadge(group));
        params.put("normal_damage_value", result.stats().normalDamageDealt());
        params.put("normal_damage_count", result.stats().normalAttackCount());
        params.put("crit_damage_value", result.stats().critDamageDealt());
        params.put("crit_damage_count", result.stats().critsCount());
        params.put("misses_count", result.stats().missesCount());
        params.put("damage_blocked_value", result.stats().damageBlocked());
        params.put("damage_blocked_count", result.stats().blockCount());
        params.put("dodged_damage_value", result.stats().damageDodged());
        params.put("dodged_damage_count", result.stats().dodgesCount());
        params.put("money_icon", Icons.MONEY);
        params.put("reward_value", result.reward().value());
        params.put("normal_attack_icon", Icons.NORMAL_ATTACK);
        params.put("crit_attack_icon", Icons.CRIT_ATTACK);
        params.put("miss_icon", Icons.MISS);
        params.put("damage_blocked_icon", Icons.BLOCKED_DAMAGE);
        params.put("dodge_icon", Icons.DODGE);
        params.put("remain_health", result.stats().remainHealth());
        params.put("total_health", result.stats().totalHealth());
        params.put("health_icon", Icons.HEALTH);
        params.put("participants_icon", Icons.PARTICIPANTS);
        params.put("remain_participants", result.stats().remainPersonages());
        params.put("total_participants", result.stats().totalPersonages());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::shortGroupBattleReport),
            params
        );
    }
}
