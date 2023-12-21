package ru.homyakin.seeker.locale.raid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.raid.RaidResult;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class RaidLocalization {
    private static final Map<Language, RaidResource> map = new HashMap<>();

    public static void add(Language language, RaidResource resource) {
        map.put(language, resource);
    }

    public static String joinRaidEvent(Language language) {
        return CommonUtils.ifNullThan(map.get(language).joinRaidEvent(), map.get(Language.DEFAULT).joinRaidEvent());
    }

    public static String raidStartsPrefix(Language language) {
        return CommonUtils.ifNullThan(map.get(language).raidStartsPrefix(), map.get(Language.DEFAULT).raidStartsPrefix());
    }

    public static String hoursShort(Language language) {
        return CommonUtils.ifNullThan(map.get(language).hoursShort(), map.get(Language.DEFAULT).minutesShort());
    }

    public static String minutesShort(Language language) {
        return CommonUtils.ifNullThan(map.get(language).minutesShort(), map.get(Language.DEFAULT).minutesShort());
    }

    public static String successJoinEvent(Language language) {
        return CommonUtils.ifNullThan(map.get(language).successJoinEvent(), map.get(Language.DEFAULT).successJoinEvent());
    }

    public static String userAlreadyInThisEvent(Language language) {
        return CommonUtils.ifNullThan(map.get(language).userAlreadyInThisEvent(), map.get(Language.DEFAULT).userAlreadyInThisEvent());
    }

    public static String userAlreadyInOtherEvent(Language language) {
        return CommonUtils.ifNullThan(map.get(language).userAlreadyInOtherEvent(), map.get(Language.DEFAULT).userAlreadyInOtherEvent());
    }

    public static String expiredRaid(Language language) {
        return CommonUtils.ifNullThan(map.get(language).expiredRaid(), map.get(Language.DEFAULT).expiredRaid());
    }

    public static String raidInProcess(Language language) {
        return CommonUtils.ifNullThan(map.get(language).raidInProcess(), map.get(Language.DEFAULT).raidInProcess());
    }

    public static String successRaid(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).successRaid(), map.get(Language.DEFAULT).successRaid())
        );
    }

    public static String failureRaid(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).failureRaid(), map.get(Language.DEFAULT).failureRaid())
        );
    }

    public static String zeroParticipants(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).zeroParticipants(), map.get(Language.DEFAULT).zeroParticipants())
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
        long totalEnemiesHealth = 0;
        long remainEnemiesHealth = 0;
        long remainingEnemies = 0;
        for (final var battlePersonage : raidResult.raidNpcResults()) {
            totalEnemiesHealth += battlePersonage.personage().characteristics().health();
            remainEnemiesHealth += battlePersonage.health();
            if (!battlePersonage.isDead()) {
                ++remainingEnemies;
            }
        }
        final var params = new HashMap<String, Object>();
        params.put("remain_enemies_health", remainEnemiesHealth);
        params.put("total_enemies_health", totalEnemiesHealth);
        params.put("remain_enemies_count", remainingEnemies);
        params.put("total_enemies_count", raidResult.raidNpcResults().size());
        params.put("top_personages_list", topPersonages.toString());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).raidResult(), map.get(Language.DEFAULT).raidResult()),
            params
        );
    }

    public static String personageRaidResult(Language language, BattlePersonage battlePersonage) {
        final var params = new HashMap<String, Object>();
        params.put("personage_icon_with_name", battlePersonage.personage().iconWithName());
        params.put("damage_dealt", battlePersonage.battleStats().damageDealt());
        params.put("damage_taken", battlePersonage.battleStats().damageTaken());
        params.put("dodges_count", battlePersonage.battleStats().dodgesCount());
        params.put("money", battlePersonage.reward().value());
        params.put("money_icon", TextConstants.MONEY_ICON);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).personageRaidResult(), map.get(Language.DEFAULT).personageRaidResult()),
            params
        );
    }

    private static final Comparator<BattlePersonage> resultComparator = Comparator.<BattlePersonage>comparingLong(
        battlePersonage -> battlePersonage.battleStats().damageDealtAndBlocked()
    ).reversed();
}
