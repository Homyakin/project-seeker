package ru.homyakin.seeker.locale.top;

import java.util.HashMap;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class TopLocalization {
    private static final Resources<TopResource> resources = new Resources<>();

    public static void add(Language language, TopResource resource) {
        resources.add(language, resource);
    }

    public static String topRaidWeek(Language language, PersonageId requestedPersonageId, TopRaidResult topRaidResult) {
        final var params = new HashMap<String, Object>();
        params.put("start_date", topRaidResult.startDate());
        params.put("end_date", topRaidResult.endDate());
        final var positions = topRaidResult.positions();
        final var positionsToShow = Math.min(MAX_TOP_POSITIONS, positions.size());
        final var personageList = new StringBuilder();
        for (int i = 1; i <= positionsToShow; ++i) {
            personageList.append(topRaidPosition(language, i, positions.get(i - 1)));
            if (i != positionsToShow) {
                personageList.append("\n");
            }
        }
        if (positionsToShow < positions.size()) {
            final var requestedIdx = topRaidResult.findPersonageIndex(requestedPersonageId);
            requestedIdx.ifPresent(
                index -> {
                    //Крайний случай, когда персонаж следующий в топе
                    if (index == positionsToShow) {
                        personageList.append("\n").append(topRaidPosition(language, index + 1, positions.get(index)));
                    } else if (index > positionsToShow) {
                        personageList
                            .append("\n-----------\n")
                            .append(topRaidPosition(language, index, positions.get(index - 1)))
                            .append("\n")
                            .append(topRaidPosition(language, index + 1, positions.get(index)));
                        if (positions.size() > index + 1) {
                            personageList.append("\n").append(topRaidPosition(language, index + 2, positions.get(index + 1)));
                        }
                    }
                }
            );
        }
        params.put("top_personage_list", personageList.toString());
        return StringNamedTemplate.format(resources.getOrDefault(language, TopResource::topRaidWeek), params);
    }

    private static String topRaidPosition(Language language, int positionNumber, TopRaidPosition position) {
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

    private static final int MAX_TOP_POSITIONS = 10;
}
