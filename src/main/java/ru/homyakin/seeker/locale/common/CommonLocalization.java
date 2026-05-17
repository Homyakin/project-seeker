package ru.homyakin.seeker.locale.common;

import ru.homyakin.seeker.game.battle.v3.two_team.BattlePersonage;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.group.passive.GroupBuildingPassiveEffect;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.event.launched.CurrentEvents;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidItem;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.event.launched.CurrentEvent;
import ru.homyakin.seeker.game.group.entity.SavedGroupBattleResult;
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
import ru.homyakin.seeker.locale.contraband.ContrabandLocalization;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
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
import java.util.List;
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
        params.put("worker_of_day_command", CommandType.WORKER_OF_DAY.getText());
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

    public static String fullProfile(
        Language language,
        Personage personage,
        CurrentEvents currentEvents,
        List<GroupPassiveEffect> groupPassiveEffects,
        Characteristics equippedCharacteristics
    ) {
        final var params = profileParams(personage, equippedCharacteristics);

        params.put("power_icon", Icons.POWER);
        params.put("power_value", LocaleUtils.power((int) new BattlePersonage(
            personage.id().value(),
            equippedCharacteristics,
            personage
        ).power()));
        params.put("item_characteristics", ItemLocalization.characteristics(language, equippedCharacteristics));
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
        params.put("personage_effects", personageProfileEffectsSection(language, personage.effects()));
        params.put("group_effects", groupProfileEffectsSection(language, groupPassiveEffects));
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

    public static String shortProfile(Language language, Personage personage, Characteristics equippedCharacteristics) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::shortProfile),
            profileParams(personage, equippedCharacteristics)
        );
    }

    private static HashMap<String, Object> profileParams(Personage personage, Characteristics equippedCharacteristics) {
        final var characteristics = equippedCharacteristics.apply(personage.effects());
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

    /**
     * Block for group chat: heading plus passive group lines (outpost, etc.).
     */
    public static String formatGroupInfoPassiveEffectsSection(
        Language language,
        List<GroupPassiveEffect> groupPassiveEffects
    ) {
        if (groupPassiveEffects.isEmpty()) {
            return "";
        }
        final var effectsText = new StringBuilder();
        for (final var passive : groupPassiveEffects) {
            effectsText.append(formatGroupPassiveEffectLine(language, passive));
        }
        if (effectsText.isEmpty()) {
            return "";
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::groupInfoPassiveEffectsSection),
            Collections.singletonMap("group_effect_lines", effectsText + "\n")
        );
    }

    private static String personageProfileEffectsSection(Language language, PersonageEffects personageEffects) {
        if (personageEffects.isEmpty()) {
            return "";
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageEffects),
            Collections.singletonMap("personage_effects", personageEffectLinesInner(language, personageEffects))
        );
    }

    private static String groupProfileEffectsSection(Language language, List<GroupPassiveEffect> groupPassiveEffects) {
        if (groupPassiveEffects.isEmpty()) {
            return "";
        }
        final var effectsText = new StringBuilder();
        for (final var passive : groupPassiveEffects) {
            effectsText.append(formatGroupPassiveEffectLine(language, passive));
        }
        if (effectsText.isEmpty()) {
            return "";
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::groupProfileEffects),
            Collections.singletonMap("group_effects", effectsText.toString())
        );
    }

    private static String formatGroupPassiveEffectLine(Language language, GroupPassiveEffect passive) {
        return switch (passive) {
            case GroupBuildingPassiveEffect gb -> groupBuildingPassiveEffectLine(language, gb);
        };
    }

    private static String personageEffectLinesInner(Language language, PersonageEffects effects) {
        final var effectsText = new StringBuilder();
        for (final var effect : effects.effects().entrySet()) {
            final var text = switch (effect.getKey()) {
                case MENU_ITEM_EFFECT -> menuItemEffect(language, effect.getValue());
                case THROW_DAMAGE_EFFECT -> throwOrderEffect(language, effect.getValue());
                case WORKER_OF_DAY_EFFECT -> workerOfDayEffect(language, effect.getValue());
                case CONTRABAND_BUFF, CONTRABAND_DEBUFF -> contrabandEffect(language, effect.getValue());
            };
            effectsText.append(text);
        }
        return effectsText.toString();
    }

    private static String groupBuildingPassiveEffectLine(Language language, GroupBuildingPassiveEffect passive) {
        final var params = new HashMap<String, Object>();
        params.put("source_label", OutpostLocalization.buildingDisplayName(language, passive.building()));
        params.put("effect", effect(language, passive.effect()));
        params.put(
            "optional_time_suffix",
            passive.expiresAt()
                .map(end -> " " + Icons.TIME + duration(language, TimeUtils.moscowTime(), end))
                .orElse("")
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::groupBuildingEffectLine),
            params
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

    private static String workerOfDayEffect(Language language, PersonageEffect effect) {
        final var params = new HashMap<String, Object>();
        params.put("effect", effect(language, effect.effect()));
        params.put("time_icon", Icons.TIME);
        params.put("duration", duration(language, TimeUtils.moscowTime(), effect.expireDateTime()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::workerOfDayEffect),
            params
        );
    }

    public static String contrabandEffect(Language language, PersonageEffect effect) {
        final var params = new HashMap<String, Object>();
        params.put("effect", effect(language, effect.effect()));
        params.put("time_icon", Icons.TIME);
        params.put("duration", duration(language, TimeUtils.moscowTime(), effect.expireDateTime()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::contrabandEffect),
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
        var d = duration;
        if (d.isNegative()) {
            d = Duration.ZERO;
        }
        var hours = "";
        if (d.toHours() > 0) {
            hours = d.toHours() + " " + hoursShort(language);
        }
        var minutes = "";
        if (d.toMinutesPart() > 0) {
            minutes = d.toMinutesPart() + " " + minutesShort(language);
        } else if (d.toHours() == 0) {
            minutes = "0 " + minutesShort(language);
        }
        if (hours.isBlank()) {
            return minutes;
        }
        return hours + " " + minutes;
    }

    public static String durationWithDays(Language language, Duration duration) {
        var d = duration;
        if (d.isNegative()) {
            d = Duration.ZERO;
        }
        final var days = d.toDays();
        final var afterDays = d.minusDays(days);
        final var hours = afterDays.toHours();
        final var afterHours = afterDays.minusHours(hours);
        final var minutes = afterHours.toMinutesPart();

        final var daysPart = days > 0 ? days + " " + daysShort(language) : "";
        var hoursPart = "";
        if (hours > 0) {
            hoursPart = hours + " " + hoursShort(language);
        }
        var minutesPart = "";
        if (minutes > 0) {
            minutesPart = minutes + " " + minutesShort(language);
        } else if (days == 0 && hours == 0) {
            minutesPart = "0 " + minutesShort(language);
        }
        final var builder = new StringBuilder();
        if (!daysPart.isEmpty()) {
            builder.append(daysPart);
        }
        if (!hoursPart.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(hoursPart);
        }
        if (!minutesPart.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(minutesPart);
        }
        return builder.toString();
    }

    public static String durationJustNow(Language language) {
        return resources.getOrDefault(language, CommonResource::durationJustNow);
    }

    private static String daysShort(Language language) {
        return resources.getOrDefault(language, CommonResource::daysShort);
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
            case Effect.RaidGoldRewardPercent raidGold -> {
                final var params = new HashMap<String, Object>();
                params.put("value", raidGold.percent());
                params.put("money_icon", Icons.MONEY);
                yield StringNamedTemplate.format(
                    resources.getOrDefault(language, CommonResource::raidGoldRewardPercentEffect),
                    params
                );
            }
            case Effect.ItemFoundChancePercent itemFound -> {
                final var params = new HashMap<String, Object>();
                params.put("value", itemFound.percent());
                yield StringNamedTemplate.format(
                    resources.getOrDefault(language, CommonResource::itemFoundChancePercentEffect),
                    params
                );
            }
        };
    }

    private static String personageInEvent(Language language, CurrentEvent event) {
        final var text = switch (event.type()) {
            case RAID -> personageInRaid(language, event.endDate());
            case PERSONAL_QUEST -> personageInQuest(language, event.endDate());
            case WORLD_RAID -> personageInWorldRaid(language, event.endDate());
        };
        final var params = new HashMap<String, Object>();
        params.put("personage_in_event", text);
        params.put(
            "cancel_command",
            CommandType.CANCEL_EVENT.getText() + TextConstants.TG_COMMAND_DELIMITER + event.id()
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageInEvent),
            params
        );
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
        params.put("raid_points", groupStats.raidPoints());
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
        params.put("worker_of_day_count", stats.workerOfDayCount());
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("outpost_building_materials", stats.outpostBuildingMaterials());
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
        params.put("worker_of_day_count", stats.workerOfDayCount());
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
        Optional<RaidItem> raidItem
    ) {
        final var params = paramsForPersonageBattleReport(result);
        params.put("battle_date_time", TimeUtils.toString(event.endDate()));
        params.put("optional_full_item", formatRaidItemFull(language, raidItem));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageBattleReport),
            params
        );
    }

    public static String shortPersonageBattleReport(
        Language language,
        PersonageBattleResult result,
        Personage personage,
        Optional<RaidItem> raidItem
    ) {
        final var params = paramsForPersonageBattleReport(result);
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("optional_short_item_without_characteristics", formatRaidItemShort(language, raidItem));
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

    public static String cancelEventSuccess(Language language, int energy) {
        final var params = new HashMap<String, Object>();
        params.put("energy_value", energy);
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::cancelEventSuccess),
            params
        );
    }

    public static String cancelEventNotFound(Language language) {
        return resources.getOrDefault(language, CommonResource::cancelEventNotFound);
    }

    public static String cancelEventLocked(Language language) {
        return resources.getOrDefault(language, CommonResource::cancelEventLocked);
    }

    private static String formatRaidItemFull(Language language, Optional<RaidItem> raidItem) {
        if (raidItem.isEmpty()) {
            return "";
        }
        return switch (raidItem.get()) {
            case RaidItem.ItemDrop itemDrop -> ItemLocalization.fullItem(language, itemDrop.item());
            case RaidItem.ContrabandDrop contrabandDrop ->
                ContrabandLocalization.contrabandDisplayForReport(language, contrabandDrop.contraband());
        };
    }

    private static String formatRaidItemShort(Language language, Optional<RaidItem> raidItem) {
        if (raidItem.isEmpty()) {
            return "";
        }
        return switch (raidItem.get()) {
            case RaidItem.ItemDrop itemDrop -> ItemLocalization.shortItemWithoutCharacteristics(language, itemDrop.item());
            case RaidItem.ContrabandDrop contrabandDrop ->
                ContrabandLocalization.contrabandDisplayForReport(language, contrabandDrop.contraband());
        };
    }
}
