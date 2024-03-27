package ru.homyakin.seeker.locale.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.event.raid.models.RaidResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageRaidSavedResult;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.TimeUtils;

public class RaidLocalization {
    private static final Resources<RaidResource> resources = new Resources<>();

    public static void add(Language language, RaidResource resource) {
        resources.add(language, resource);
    }

    public static String joinRaidEvent(Language language) {
        return resources.getOrDefault(language, RaidResource::joinRaidEvent);
    }

    public static String raidStartsPrefix(Language language) {
        return resources.getOrDefault(language, RaidResource::raidStartsPrefix);
    }

    public static String hoursShort(Language language) {
        return resources.getOrDefault(language, RaidResource::hoursShort);
    }

    public static String minutesShort(Language language) {
        return resources.getOrDefault(language, RaidResource::minutesShort);
    }

    public static String userAlreadyInThisEvent(Language language) {
        return resources.getOrDefault(language, RaidResource::userAlreadyInThisEvent);
    }

    public static String userAlreadyInOtherEvent(Language language) {
        return resources.getOrDefault(language, RaidResource::userAlreadyInOtherEvent);
    }

    public static String expiredRaid(Language language) {
        return resources.getOrDefault(language, RaidResource::expiredRaid);
    }

    public static String raidInProcess(Language language) {
        return resources.getOrDefault(language, RaidResource::raidInProcess);
    }

    public static String successRaid(Language language) {
        return resources.getOrDefaultRandom(language, RaidResource::successRaid);
    }

    public static String failureRaid(Language language) {
        return resources.getOrDefaultRandom(language, RaidResource::failureRaid);
    }

    public static String zeroParticipants(Language language) {
        return resources.getOrDefaultRandom(language, RaidResource::zeroParticipants);
    }

    public static String raidResult(Language language, RaidResult raidResult) {
        final var sortedPersonages = new ArrayList<>(raidResult.personageResults());
        sortedPersonages.sort(resultComparator);
        final var topPersonages = new StringBuilder();
        final int topCount = Math.min(5, sortedPersonages.size());
        for (int i = 0; i < topCount; ++i) {
            topPersonages
                .append(i + 1)
                .append(". ")
                .append(RaidLocalization.personageRaidResult(language, sortedPersonages.get(i)));
            if (i < topCount - 1) {
                topPersonages.append("\n");
            }
        }
        final var params = paramsForRaidResult(language, raidResult, topPersonages);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, RaidResource::raidResult),
            params
        );
    }

    private static HashMap<String, Object> paramsForRaidResult(
        Language language,
        RaidResult raidResult,
        StringBuilder topPersonages
    ) {
        long totalEnemiesHealth = 0;
        long remainEnemiesHealth = 0;
        long remainingEnemies = 0;
        for (final var result : raidResult.raidNpcResults()) {
            totalEnemiesHealth += result.personage().calcTotalCharacteristics().health();
            remainEnemiesHealth += result.stats().remainHealth();
            if (!result.stats().isDead()) {
                ++remainingEnemies;
            }
        }
        final var params = new HashMap<String, Object>();
        params.put("remain_enemies_health", remainEnemiesHealth);
        params.put("total_enemies_health", totalEnemiesHealth);
        params.put("remain_enemies_count", remainingEnemies);
        params.put("total_enemies_count", raidResult.raidNpcResults().size());
        params.put("top_personages_list", topPersonages.toString());
        params.put("raid_report_command", CommandType.RAID_REPORT.getText());
        if (raidResult.generatedItemResults().isEmpty()) {
            params.put("from_new_line_items_for_personages", "");
        } else {
            final var builder = new StringBuilder("\n\n");
            for (final var result: raidResult.generatedItemResults()) {
                builder.append(itemResult(language, result)).append("\n");
            }
            params.put("from_new_line_items_for_personages", builder.toString());
        }
        return params;
    }

    public static String itemResult(Language language, GeneratedItemResult generatedItemResult) {
        final var params = new HashMap<String, Object>();
        final var text = switch (generatedItemResult) {
            case GeneratedItemResult.Success success -> {
                params.put("personage_badge_with_name", success.personage().badgeWithName());
                params.put("short_item", ItemLocalization.shortItem(language, success.item()));
                yield resources.getOrDefaultRandom(language, RaidResource::successItemForPersonage);
            }
            case GeneratedItemResult.NotEnoughSpaceInBag notEnoughSpaceInBag -> {
                params.put("personage_badge_with_name", notEnoughSpaceInBag.personage().badgeWithName());
                params.put("short_item", ItemLocalization.shortItem(language, notEnoughSpaceInBag.item()));
                yield resources.getOrDefaultRandom(language, RaidResource::notEnoughSpaceInBagForItem);
            }
        };
        return StringNamedTemplate.format(text, params);
    }
    public static String personageRaidResult(Language language, PersonageRaidResult result) {
        final var params = new HashMap<String, Object>();
        params.put("dead_icon_or_empty", result.stats().isDead() ? Icons.DEAD : "");
        params.put("personage_badge_with_name", result.personage().badgeWithName());
        params.put("damage_dealt", result.stats().damageDealt());
        params.put("damage_taken", result.stats().damageTaken());
        params.put("money", result.reward().value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, RaidResource::personageRaidResult),
            params
        );
    }

    public static String raidParticipants(Language language, List<Personage> participants) {
        final var iconNames = participants.stream()
            .map(Personage::badgeWithName)
            .collect(Collectors.joining(", "));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, RaidResource::raidParticipants),
            Collections.singletonMap("personage_badge_name_list", iconNames)
        );
    }

    public static String notEnoughEnergy(Language language, PersonageEventError.NotEnoughEnergy notEnoughEnergy) {
        final var params = new HashMap<String, Object>();
        params.put("energy_icon", Icons.ENERGY);
        params.put("required_energy", notEnoughEnergy.requiredEnergy());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, RaidResource::notEnoughEnergy),
            params
        );
    }

    public static String report(Language language, PersonageRaidSavedResult result, LaunchedEvent event) {
        final var params = paramsForRaidReport(result);
        params.put("raid_date_time", TimeUtils.toString(event.endDate()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, RaidResource::report),
            params
        );
    }

    public static String shortPersonageReport(Language language, PersonageRaidSavedResult result, Personage personage) {
        final var params = paramsForRaidReport(result);
        params.put("personage_badge_with_name", personage.badgeWithName());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, RaidResource::shortPersonageReport),
            params
        );
    }

    public static String reportNotPresentForPersonage(Language language) {
        return resources.getOrDefault(language, RaidResource::reportNotPresentForPersonage);
    }

    public static String lastGroupRaidReportNotFound(Language language) {
        return resources.getOrDefault(language, RaidResource::lastGroupRaidReportNotFound);
    }

    private static HashMap<String, Object> paramsForRaidReport(PersonageRaidSavedResult result) {
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

    private static final Comparator<PersonageRaidResult> resultComparator = Comparator.<PersonageRaidResult>comparingLong(
        result -> result.stats().damageDealtAndTaken()
    ).reversed();
}
