package ru.homyakin.seeker.locale.raid;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.CommonUtils;

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

    public static String expiredEvent(Language language) {
        return CommonUtils.ifNullThan(map.get(language).expiredEvent(), map.get(Language.DEFAULT).expiredEvent());
    }

    public static String successRaid(Language language) {
        return CommonUtils.ifNullThan(map.get(language).successRaid(), map.get(Language.DEFAULT).successRaid());
    }

    public static String failureRaid(Language language) {
        return CommonUtils.ifNullThan(map.get(language).failureRaid(), map.get(Language.DEFAULT).failureRaid());
    }
}
