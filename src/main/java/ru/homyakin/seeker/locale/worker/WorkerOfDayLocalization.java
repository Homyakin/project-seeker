package ru.homyakin.seeker.locale.worker;

import java.util.Collections;
import java.util.HashMap;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class WorkerOfDayLocalization {
    private static final Resources<WorkerOfDayResource> resources = new Resources<>();

    public static void add(Language language, WorkerOfDayResource resource) {
        resources.add(language, resource);
    }

    public static String notEnoughUsers(Language language, int requiredUsers) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorkerOfDayResource::notEnoughUsers),
            Collections.singletonMap("required_users", requiredUsers)
        );
    }

    public static String alreadyChosen(Language language, PersonageMention mention) {
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, WorkerOfDayResource::alreadyChosen),
            Collections.singletonMap("mention_personage_badge_with_name", mention.value())
        );
    }

    public static String chosenUser(Language language, PersonageMention mention, Effect effect) {
        final var params = new HashMap<String, Object>();
        params.put("chosen_user_text", chosenUserVariation(language, mention));
        params.put("effect", CommonLocalization.effect(language, effect));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorkerOfDayResource::chosenUser),
            params
        );
    }

    private static String chosenUserVariation(Language language, PersonageMention mention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_personage_badge_with_name", mention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, WorkerOfDayResource::chosenUserVariations),
            params
        );
    }
}
