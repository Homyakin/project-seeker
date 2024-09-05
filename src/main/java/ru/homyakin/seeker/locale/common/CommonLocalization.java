package ru.homyakin.seeker.locale.common;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.personage.models.CurrentEvent;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageEffects;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemEffect;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.group.stats.GroupPersonageStats;
import ru.homyakin.seeker.telegram.group.stats.GroupStats;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

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

    public static String onlyAdminAction(Language language) {
        return resources.getOrDefault(language, CommonResource::onlyAdminAction);
    }

    public static String internalError(Language language) {
        return resources.getOrDefault(language, CommonResource::internalError);
    }

    public static String fullProfile(Language language, Personage personage) {
        final var params = profileParams(language, personage);

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
        if (personage.currentEvent().isEmpty()) {
            params.put("current_event", "");
        } else {
            params.put("current_event", personageInEvent(language, personage.currentEvent().get()));
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::fullProfile),
            params
        );
    }

    public static String shortProfile(Language language, Personage personage) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::shortProfile),
            profileParams(language, personage)
        );
    }

    private static HashMap<String, Object> profileParams(Language language, Personage personage) {
        final var characteristics = personage.calcTotalCharacteristicsWithEffects();
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

    private static String personageEffects(Language language, PersonageEffects effects) {
        if (effects.menuItemEffect().isEmpty()) {
            return "";
        }
        final var effect = effects.menuItemEffect().get();
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::personageEffects),
            Collections.singletonMap("personage_effects", menuItemEffect(language, effect))
        );
    }

    private static String menuItemEffect(Language language, MenuItemEffect effect) {
        final var params = new HashMap<String, Object>();
        params.put("effect", effect(language, effect.effect()));
        params.put("time_icon", Icons.TIME);
        params.put("duration", duration(language, TimeUtils.moscowTime(), effect.expireDateTime()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CommonResource::menuItemEffect),
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

    private static String effect(Language language, Effect effect) {
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
        };
    }

    private static String personageInEvent(Language language, CurrentEvent event) {
        return switch (event.type()) {
            case RAID -> personageInRaid(language, event.endDate());
            case PERSONAL_QUEST -> personageInQuest(language, event.endDate());
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

    public static String receptionDesk(Language language) {
        return resources.getOrDefault(language, CommonResource::receptionDesk);
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

    public static String migrateGroup(Language language) {
        return resources.getOrDefault(language, CommonResource::migrateGroup);
    }
}
