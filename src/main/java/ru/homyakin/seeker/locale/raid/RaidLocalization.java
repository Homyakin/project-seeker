package ru.homyakin.seeker.locale.raid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import ru.homyakin.seeker.game.battle.PersonageResult;
import ru.homyakin.seeker.game.event.raid.RaidResult;
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
            topPersonages.append(i + 1).append(". ").append(sortedPersonages.get(i).statsText(language)).append("\n");
        }
        long totalEnemyHealth = 0;
        long remainEnemyHealth = 0;
        for (final var result: raidResult.raidNpcResults()) {
            totalEnemyHealth += result.personage().characteristics().health();
            remainEnemyHealth += result.battlePersonage().health();
        }
        final var params = new HashMap<String, Object>();
        params.put("remain_enemy_health", remainEnemyHealth);
        params.put("total_enemy_health", totalEnemyHealth);
        params.put("top_personages_list", topPersonages.toString());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).raidResult(), map.get(Language.DEFAULT).raidResult()),
            params
        );
    }

    private static final Comparator<PersonageResult> resultComparator = Comparator.<PersonageResult>comparingLong(
        personageResult -> personageResult.battlePersonage().battleStats().damageDealtAndBlocked()
    ).reversed();
}
