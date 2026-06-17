package ru.homyakin.seeker.locale.worker;

import java.util.Collections;
import java.util.HashMap;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class WorkerOfDayLocalization {
    private static final Resources<WorkerOfDayResource> resources = new Resources<>();

    public static void add(Language language, WorkerOfDayResource resource) {
        resources.add(language, resource);
    }

    public static String notEnoughMembers(Language language, int requiredUsers) {
        final var params = new HashMap<String, Object>();
        params.put("required_users", requiredUsers);
        params.put("group_join_command", CommandType.JOIN_GROUP.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorkerOfDayResource::notEnoughMembers),
            params
        );
    }

    public static String alreadyChosen(Language language, PersonageMention mention) {
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, WorkerOfDayResource::alreadyChosen),
            Collections.singletonMap("mention_personage_badge_with_name", mention.value())
        );
    }

    public static String chosenMember(Language language, PersonageMention mention, Effect effect) {
        final var params = new HashMap<String, Object>();
        params.put("chosen_member_text", chosenMemberVariation(language, mention));
        params.put("effect", CommonLocalization.effect(language, effect));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorkerOfDayResource::chosenMember),
            params
        );
    }

    private static String chosenMemberVariation(Language language, PersonageMention mention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_personage_badge_with_name", mention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, WorkerOfDayResource::chosenMemberVariations),
            params
        );
    }
}
