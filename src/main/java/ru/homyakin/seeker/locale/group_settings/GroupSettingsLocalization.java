package ru.homyakin.seeker.locale.group_settings;

import java.util.HashMap;
import ru.homyakin.seeker.game.group.entity.EventInterval;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.game.group.entity.GroupSettings;
import ru.homyakin.seeker.game.group.error.IncorrectTimeZone;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class GroupSettingsLocalization {
    private static final Resources<GroupSettingsResource> resources = new Resources<>();

    public static void add(Language language, GroupSettingsResource resource) {
        resources.add(language, resource);
    }

    public static String groupSettings(Language language, GroupSettings settings) {
        final var params = new HashMap<String, Object>();
        params.put("time_zone", settings.timeZone());
        params.put("set_time_zone_command", CommandType.SET_TIME_ZONE.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupSettingsResource::groupSettings),
            params
        );
    }

    public static String eventIntervalButton(Language language, EventInterval interval) {
        final var params = new HashMap<String, Object>();
        params.put("start_hour", interval.startHour());
        params.put("end_hour", interval.endHour());
        params.put("enabled_icon", interval.isEnabled() ? Icons.ENABLED : Icons.DISABLED);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupSettingsResource::eventIntervalButton),
            params
        );
    }

    public static String incorrectTimeZone(Language language, IncorrectTimeZone error) {
        final var params = new HashMap<String, Object>();
        params.put("min_time_zone", error.min());
        params.put("max_time_zone", error.max());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupSettingsResource::incorrectTimeZone),
            params
        );
    }

    public static String zeroEnabledEventIntervals(Language language) {
        return resources.getOrDefault(language, GroupSettingsResource::zeroEnabledEventIntervals);
    }

    public static String incorrectSetTimeZoneCommand(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("set_time_zone_command", CommandType.SET_TIME_ZONE.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupSettingsResource::incorrectSetTimeZoneCommand),
            params
        );
    }

    public static String successChangeTimeZone(Language language) {
        return resources.getOrDefault(language, GroupSettingsResource::successChangeTimeZone);
    }
}
