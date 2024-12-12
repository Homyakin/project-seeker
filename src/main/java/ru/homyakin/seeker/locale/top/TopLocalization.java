package ru.homyakin.seeker.locale.top;

import java.util.HashMap;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.GroupTopRaidPosition;
import ru.homyakin.seeker.game.top.models.GroupTopRaidResult;
import ru.homyakin.seeker.game.top.models.TopPowerPersonagePosition;
import ru.homyakin.seeker.game.top.models.TopPowerPersonageResult;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.game.top.models.TopSpinPosition;
import ru.homyakin.seeker.game.top.models.TopSpinResult;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class TopLocalization {
    private static final Resources<TopResource> resources = new Resources<>();

    public static void add(Language language, TopResource resource) {
        resources.add(language, resource);
    }

    public static String topRaidWeek(Language language, PersonageId requestedPersonageId, TopRaidResult topRaidResult) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TopResource::topRaidWeek),
            topRaidWeekParams(language, requestedPersonageId, topRaidResult)
        );
    }

    public static String topRaidWeekGroup(Language language, PersonageId requestedPersonageId, TopRaidResult topRaidResult) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TopResource::topRaidWeekGroup),
            topRaidWeekParams(language, requestedPersonageId, topRaidResult)
        );
    }

    private static HashMap<String, Object> topRaidWeekParams(
        Language language,
        PersonageId requestedPersonageId,
        TopRaidResult topRaidResult
    ) {
        final var params = new HashMap<String, Object>();
        params.put("start_date", topRaidResult.startDate());
        params.put("end_date", topRaidResult.endDate());
        final var topPersonageList = TopUtils.createTopList(language, requestedPersonageId, topRaidResult);
        params.put("top_personage_list", topPersonageList);
        params.put("total_count", topRaidResult.positions().size());
        return params;
    }

    public static String topRaidPosition(Language language, int positionNumber, TopRaidPosition position) {
        final var params = new HashMap<String, Object>();
        params.put("position", positionNumber);
        params.put("personage_badge_with_name", position.personageBadgeWithName());
        params.put("success_raids", position.successRaids());
        params.put("all_raids", position.successRaids() + position.failedRaids());
        return StringNamedTemplate.format(resources.getOrDefault(language, TopResource::topRaidPosition), params);
    }

    public static String topRaidEmpty(Language language) {
        return resources.getOrDefault(language, TopResource::topRaidEmpty);
    }

    public static String topList(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("top_work_group_command", CommandType.SPIN_TOP.getText());
        params.put("top_raid_week_command", CommandType.TOP_RAID_WEEK.getText());
        params.put("top_raid_week_group_command", CommandType.TOP_RAID_WEEK_GROUP.getText());
        params.put("top_group_raid_week_command", CommandType.TOP_GROUP_RAID_WEEK.getText());
        params.put("top_power_personage_command", CommandType.TOP_POWER_GROUP.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TopResource::topList),
            params
        );
    }

    public static String topSpinEmpty(Language language) {
        return resources.getOrDefault(language, TopResource::topSpinEmpty);
    }

    public static String topSpinGroup(Language language, PersonageId requestedPersonageId, TopSpinResult topSpinResult) {
        final var params = new HashMap<String, Object>();
        final var topPersonageList = TopUtils.createTopList(language, requestedPersonageId, topSpinResult);
        params.put("top_personage_list", topPersonageList);
        params.put("total_count", topSpinResult.positions().size());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TopResource::topSpinGroup),
            params
        );
    }

    public static String topSpinPosition(Language language, int positionNumber, TopSpinPosition position) {
        final var params = new HashMap<String, Object>();
        params.put("position", positionNumber);
        params.put("personage_badge_with_name", position.personageBadgeWithName());
        params.put("work_count", position.workCount());
        return StringNamedTemplate.format(resources.getOrDefault(language, TopResource::topSpinPosition), params);
    }

    public static String topGroupRaidWeek(
        Language language,
        GroupId requestedGroupId,
        GroupTopRaidResult result
    ) {
        final var params = new HashMap<String, Object>();
        params.put("start_date", result.startDate());
        params.put("end_date", result.endDate());
        final var topGroupList = TopUtils.createTopList(language, requestedGroupId, result);
        params.put("top_group_list", topGroupList);
        params.put("total_count", result.positions().size());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TopResource::topGroupRaidWeek),
            params
        );
    }

    public static String topGroupRaidPosition(
        Language language,
        int positionNumber,
        GroupTopRaidPosition position
    ) {
        final var params = new HashMap<String, Object>();
        params.put("position", positionNumber);
        params.put("group_badge_with_name", LocaleUtils.groupNameWithBadge(position));
        params.put("success_raids", position.successRaids());
        params.put("all_raids", position.successRaids() + position.failedRaids());
        return StringNamedTemplate.format(resources.getOrDefault(language, TopResource::topGroupRaidPosition), params);
    }

    public static String topPowerPersonageGroup(
        Language language,
        PersonageId requestedPersonageId,
        TopPowerPersonageResult result
    ) {
        final var params = new HashMap<String, Object>();
        final var topPersonageList = TopUtils.createTopList(language, requestedPersonageId, result);
        params.put("top_personage_list", topPersonageList);
        params.put("total_count", result.positions().size());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TopResource::topPowerPersonageGroup),
            params
        );
    }

    public static String topPowerPersonagePosition(
        Language language,
        int positionNumber,
        TopPowerPersonagePosition position
    ) {
        final var params = new HashMap<String, Object>();
        params.put("position", positionNumber);
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(position));
        params.put("power", position.power() / 100); // делим на 100, так как слишком большие числа (32052 в дефолте)
        return StringNamedTemplate.format(resources.getOrDefault(language, TopResource::topPowerPersonagePosition), params);
    }

    public static String topPersonageEmpty(Language language) {
        return resources.getOrDefault(language, TopResource::topPersonageEmpty);
    }
}
