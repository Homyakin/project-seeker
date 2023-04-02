package ru.homyakin.seeker.locale.group_settings;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.ActiveTime;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ActiveTimeLocalization {
    private static final Map<Language, ActiveTimeResource> map = new HashMap<>();

    public static void add(Language language, ActiveTimeResource resource) {
        map.put(language, resource);
    }

    public static String incorrectArgumentsNumber(Language language) {
        return CommonUtils.ifNullThan(map.get(language).incorrectArgumentsNumber(), map.get(Language.DEFAULT).incorrectArgumentsNumber());
    }

    public static String argumentsNotANumber(Language language) {
        return CommonUtils.ifNullThan(map.get(language).argumentsNotANumber(), map.get(Language.DEFAULT).argumentsNotANumber());
    }

    public static String successChange(Language language) {
        return CommonUtils.ifNullThan(map.get(language).successChange(), map.get(Language.DEFAULT).successChange());
    }

    public static String incorrectHour(Language language) {
        return CommonUtils.ifNullThan(map.get(language).incorrectHour(), map.get(Language.DEFAULT).incorrectHour());
    }

    public static String startMoreThanEnd(Language language) {
        return CommonUtils.ifNullThan(map.get(language).startMoreThanEnd(), map.get(Language.DEFAULT).startMoreThanEnd());
    }

    public static String incorrectTimeZone(Language language, int minTimeZone, int maxTimeZone) {
        final var params = new HashMap<String, Object>();
        params.put("min_time_zone", minTimeZone);
        params.put("max_time_zone", maxTimeZone);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).incorrectTimeZone(), map.get(Language.DEFAULT).incorrectTimeZone()),
            params
        );
    }

    public static String current(Language language, ActiveTime activeTime) {
        final var params = new HashMap<String, Object>();
        params.put("active_time", activeTime.toString());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).current(), map.get(Language.DEFAULT).current()),
            params
        );
    }
}
