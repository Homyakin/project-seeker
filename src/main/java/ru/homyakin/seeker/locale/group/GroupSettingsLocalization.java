package ru.homyakin.seeker.locale.group;

import ru.homyakin.seeker.game.group.entity.EventInterval;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.error.IncorrectTimeZone;
import ru.homyakin.seeker.game.utils.NameError;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.util.Collections;
import java.util.HashMap;

public class GroupSettingsLocalization {
    private static final Resources<GroupSettingsResource> resources = new Resources<>();

    public static void add(Language language, GroupSettingsResource resource) {
        resources.add(language, resource);
    }

    public static String groupSettings(Language language, Group group) {
        final var params = new HashMap<String, Object>();
        params.put("time_zone", group.settings().timeZone());
        params.put("set_time_zone_command", CommandType.SET_TIME_ZONE.getText());
        params.put("change_group_name_command", CommandType.CHANGE_GROUP_NAME.getText());
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put(
            "optional_group_is_hidden_description",
            groupIsHiddenDescription(language, group.settings().isHidden())
        );
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

    public static String changeNameInvalidFormat(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupSettingsResource::changeNameInvalidFormat),
            Collections.singletonMap("change_group_name_command", CommandType.CHANGE_GROUP_NAME.getText())
        );
    }

    public static String changeNameInvalidSymbols(Language language) {
        return resources.getOrDefault(language, GroupSettingsResource::changeNameInvalidSymbols);
    }

    public static String changeNameInvalidLength(Language language, NameError.InvalidLength error) {
        final var params = new HashMap<String, Object>();
        params.put("min_name_length", error.minLength());
        params.put("max_name_length", error.maxLength());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupSettingsResource::changeNameInvalidLength),
            params
        );
    }

    public static String successChangeName(Language language, String name) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupSettingsResource::successChangeName),
            Collections.singletonMap("name", name)
        );
    }

    public static String groupIsHidden(Language language) {
        return resources.getOrDefault(language, GroupSettingsResource::groupIsHidden);
    }

    public static String groupIsNotHidden(Language language) {
        return resources.getOrDefault(language, GroupSettingsResource::groupIsNotHidden);
    }

    public static String forbiddenHidden(Language language) {
        return resources.getOrDefault(language, GroupSettingsResource::forbiddenHidden);
    }

    private static String groupIsHiddenDescription(Language language, boolean isHidden) {
        if (isHidden) {
            return resources.getOrDefault(language, GroupSettingsResource::groupIsHiddenDescription);
        } else {
            return "";
        }
    }
}
