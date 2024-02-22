package ru.homyakin.seeker.locale.top;

import java.util.HashMap;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.locale.Language;
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
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TopResource::topList),
            params
        );
    }
}
