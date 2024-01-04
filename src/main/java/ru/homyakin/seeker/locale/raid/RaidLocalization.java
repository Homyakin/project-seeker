package ru.homyakin.seeker.locale.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.event.raid.models.RaidResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageRaidSavedResult;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.TimeUtils;

public class RaidLocalization {
    private static final Map<Language, RaidResource> map = new HashMap<>();

    public static void add(Language language, RaidResource resource) {
        map.put(language, resource);
    }

    public static String joinRaidEvent(Language language) {
        return CommonUtils.ifNullThen(map.get(language).joinRaidEvent(), map.get(Language.DEFAULT).joinRaidEvent());
    }

    public static String raidStartsPrefix(Language language) {
        return CommonUtils.ifNullThen(map.get(language).raidStartsPrefix(), map.get(Language.DEFAULT).raidStartsPrefix());
    }

    public static String hoursShort(Language language) {
        return CommonUtils.ifNullThen(map.get(language).hoursShort(), map.get(Language.DEFAULT).minutesShort());
    }

    public static String minutesShort(Language language) {
        return CommonUtils.ifNullThen(map.get(language).minutesShort(), map.get(Language.DEFAULT).minutesShort());
    }

    public static String userAlreadyInThisEvent(Language language) {
        return CommonUtils.ifNullThen(map.get(language).userAlreadyInThisEvent(), map.get(Language.DEFAULT).userAlreadyInThisEvent());
    }

    public static String userAlreadyInOtherEvent(Language language) {
        return CommonUtils.ifNullThen(map.get(language).userAlreadyInOtherEvent(), map.get(Language.DEFAULT).userAlreadyInOtherEvent());
    }

    public static String expiredRaid(Language language) {
        return CommonUtils.ifNullThen(map.get(language).expiredRaid(), map.get(Language.DEFAULT).expiredRaid());
    }

    public static String raidInProcess(Language language) {
        return CommonUtils.ifNullThen(map.get(language).raidInProcess(), map.get(Language.DEFAULT).raidInProcess());
    }

    public static String successRaid(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThen(map.get(language).successRaid(), map.get(Language.DEFAULT).successRaid())
        );
    }

    public static String failureRaid(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThen(map.get(language).failureRaid(), map.get(Language.DEFAULT).failureRaid())
        );
    }

    public static String zeroParticipants(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThen(map.get(language).zeroParticipants(), map.get(Language.DEFAULT).zeroParticipants())
        );
    }

    public static String raidResult(Language language, RaidResult raidResult) {
        final var sortedPersonages = new ArrayList<>(raidResult.personageResults());
        sortedPersonages.sort(resultComparator);
        final var topPersonages = new StringBuilder();
        for (int i = 0; i < 5 && i < sortedPersonages.size(); ++i) {
            topPersonages
                .append(i + 1)
                .append(". ")
                .append(RaidLocalization.personageRaidResult(language, sortedPersonages.get(i)))
                .append("\n");
        }
        final var params = paramsForRaidResult(raidResult, topPersonages);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).raidResult(), map.get(Language.DEFAULT).raidResult()),
            params
        );
    }

    private static HashMap<String, Object> paramsForRaidResult(RaidResult raidResult, StringBuilder topPersonages) {
        long totalEnemiesHealth = 0;
        long remainEnemiesHealth = 0;
        long remainingEnemies = 0;
        for (final var result : raidResult.raidNpcResults()) {
            totalEnemiesHealth += result.personage().characteristics().health();
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
        return params;
    }

    public static String personageRaidResult(Language language, PersonageRaidResult result) {
        final var params = new HashMap<String, Object>();
        params.put("dead_icon_or_empty", result.stats().isDead() ? TextConstants.DEAD_ICON : "");
        params.put("personage_icon_with_name", result.personage().iconWithName());
        params.put("damage_dealt", result.stats().damageDealt());
        params.put("damage_taken", result.stats().damageTaken());
        params.put("money", result.reward().value());
        params.put("money_icon", TextConstants.MONEY_ICON);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).personageRaidResult(), map.get(Language.DEFAULT).personageRaidResult()),
            params
        );
    }

    public static String raidParticipants(Language language, List<Personage> participants) {
        final var iconNames = participants.stream()
            .map(Personage::iconWithName)
            .collect(Collectors.joining(", "));
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).raidParticipants(), map.get(Language.DEFAULT).raidParticipants()),
            Collections.singletonMap("personage_icon_name_list", iconNames)
        );
    }

    public static String notEnoughEnergy(Language language, PersonageEventError.NotEnoughEnergy notEnoughEnergy) {
        final var params = new HashMap<String, Object>();
        params.put("energy_icon", TextConstants.ENERGY_ICON);
        params.put("required_energy", notEnoughEnergy.requiredEnergy());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).notEnoughEnergy(), map.get(Language.DEFAULT).notEnoughEnergy()),
            params
        );
    }

    public static String report(Language language, PersonageRaidSavedResult result, LaunchedEvent event) {
        final var params = new HashMap<String, Object>();
        params.put("raid_date_time", TimeUtils.toString(event.endDate()));
        params.put("attack_icon", TextConstants.ATTACK_ICON);
        params.put("attack_value", result.stats().characteristics().attack());
        params.put("defense_icon", TextConstants.DEFENSE_ICON);
        params.put("defense_value", result.stats().characteristics().defense());
        params.put("strength_icon", TextConstants.STRENGTH_ICON);
        params.put("strength_value", result.stats().characteristics().strength());
        params.put("agility_icon", TextConstants.AGILITY_ICON);
        params.put("agility_value", result.stats().characteristics().agility());
        params.put("wisdom_icon", TextConstants.WISDOM_ICON);
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
        params.put("money_icon", TextConstants.MONEY_ICON);
        params.put("reward_value", result.reward().value());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).report(), map.get(Language.DEFAULT).report()),
            params
        );
    }

    public static String reportNotPresent(Language language) {
        return CommonUtils.ifNullThen(map.get(language).reportNotPresent(), map.get(Language.DEFAULT).reportNotPresent());
    }

    private static final Comparator<PersonageRaidResult> resultComparator = Comparator.<PersonageRaidResult>comparingLong(
        result -> result.stats().damageDealtAndTaken()
    ).reversed();
}
