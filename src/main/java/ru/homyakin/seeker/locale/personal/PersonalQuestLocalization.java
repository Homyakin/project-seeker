package ru.homyakin.seeker.locale.personal;

import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestRequirements;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.util.Collections;
import java.util.HashMap;

public class PersonalQuestLocalization {
    private static final Resources<PersonalQuestResource> resources = new Resources<>();

    public static void add(Language language, PersonalQuestResource resource) {
        resources.add(language, resource);
    }

    public static String bulletinBoard(Language language, PersonalQuestRequirements requirements) {
        final var params = new HashMap<String, Object>();
        params.put("required_energy", requirements.requiredEnergy());
        params.put("energy_icon", Icons.ENERGY);
        params.put("time_icon", Icons.TIME);
        params.put("duration", CommonLocalization.duration(language, requirements.requiredTime()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, PersonalQuestResource::bulletinBoard),
            params
        );
    }

    public static String notEnoughEnergy(Language language, int requiredEnergy) {
        final var params = new HashMap<String, Object>();
        params.put("required_energy", requiredEnergy);
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, PersonalQuestResource::notEnoughEnergy),
            params
        );
    }

    public static String personageInAnotherEvent(Language language) {
        return resources.getOrDefault(language, PersonalQuestResource::personageInAnotherEvent);
    }

    public static String startedQuest(Language language, StartedQuest startedQuest) {
        final var params = new HashMap<String, Object>();
        params.put("quest_intro", startedQuest.quest().getLocaleOrDefault(language).intro());
        params.put("time_icon", Icons.TIME);
        params.put("duration", CommonLocalization.duration(language, startedQuest.duration()));
        params.put("energy_icon", Icons.ENERGY);
        params.put("energy", startedQuest.takenEnergy());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, PersonalQuestResource::startedQuest),
            params
        );
    }

    public static String autoStartedQuest(Language language, StartedQuest startedQuest) {
        final var params = new HashMap<String, Object>();
        params.put("energy_recovered", CommonLocalization.energyRecovered(language));
        params.put("started_quest", startedQuest(language, startedQuest));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, PersonalQuestResource::autoStartedQuest),
            params
        );
    }

    public static String failedQuest(Language language, EventResult.PersonalQuestResult.Failure result) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, PersonalQuestResource::failedQuest),
            Collections.singletonMap("quest_failure", result.quest().getLocaleOrDefault(language).failure())
        );
    }

    public static String successQuest(Language language, EventResult.PersonalQuestResult.Success result) {
        final var params = new HashMap<String, Object>();
        params.put("quest_success", result.quest().getLocaleOrDefault(language).success());
        params.put("money_icon", Icons.MONEY);
        params.put("money_value", result.reward().value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, PersonalQuestResource::successQuest),
            params
        );
    }
}
