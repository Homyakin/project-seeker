package ru.homyakin.seeker.locale.group_settings;

import java.util.HashMap;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.group.models.ActiveTime;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ActiveTimeLocalization {
    private static final Resources<ActiveTimeResource> resources = new Resources<>();

    public static void add(Language language, ActiveTimeResource resource) {
        resources.add(language, resource);
    }

    public static String incorrectArgumentsNumber(Language language) {
        return resources.getOrDefault(language, ActiveTimeResource::incorrectArgumentsNumber);
    }

    public static String argumentsNotANumber(Language language) {
        return resources.getOrDefault(language, ActiveTimeResource::argumentsNotANumber);
    }

    public static String successChange(Language language) {
        return resources.getOrDefault(language, ActiveTimeResource::successChange);
    }

    public static String incorrectHour(Language language) {
        return resources.getOrDefault(language, ActiveTimeResource::incorrectHour);
    }

    public static String startMoreThanEnd(Language language) {
        return resources.getOrDefault(language, ActiveTimeResource::startMoreThanEnd);
    }

    public static String incorrectTimeZone(Language language, int minTimeZone, int maxTimeZone) {
        final var params = new HashMap<String, Object>();
        params.put("min_time_zone", minTimeZone);
        params.put("max_time_zone", maxTimeZone);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ActiveTimeResource::incorrectTimeZone),
            params
        );
    }

    public static String current(Language language, ActiveTime activeTime) {
        final var params = new HashMap<String, Object>();
        params.put("active_time", activeTime.toString());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ActiveTimeResource::current),
            params
        );
    }
}
