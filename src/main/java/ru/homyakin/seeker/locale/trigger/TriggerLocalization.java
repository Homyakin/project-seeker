package ru.homyakin.seeker.locale.trigger;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonResource;

public class TriggerLocalization {

    private static final Resources<TriggerResource> resources = new Resources<>();

    public static void add(Language language, TriggerResource resource) {
        resources.add(language, resource);
    }

    public static String triggerCreated(Language language) {
        return resources.getOrDefault(language, TriggerResource::triggerCreated);
    }

    public static String triggerNotFound(Language language) {
        return resources.getOrDefault(language, TriggerResource::triggerNotFound);
    }

    public static String triggerDeleted(Language language) {
        return resources.getOrDefault(language, TriggerResource::triggerDeleted);
    }

    public static String noTriggerText(Language language) {
        return resources.getOrDefault(language, TriggerResource::noTriggerText);
    }

    public static String unknownException(Language language) {
        return resources.getOrDefault(language, TriggerResource::unknownException);
    }
}
