package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ChangeNameLocalization {
    private static final Resources<ChangeNameResource> resources = new Resources<>();

    public static void add(Language language, ChangeNameResource resource) {
        resources.add(language, resource);
    }

    public static String changeNameWithoutName(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::changeNameWithoutName);
    }

    public static String initChangeName(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::initChangeName);
    }

    public static String cancelChangeName(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::cancelChangeName);
    }

    public static String confirmName(Language language, String name) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ChangeNameResource::confirmName),
            Collections.singletonMap("name", name)
        );
    }

    public static String cancelButton(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::cancelButton);
    }

    public static String confirmButton(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::confirmButton);
    }

    public static String repeatButton(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::repeatButton);
    }

    public static String personageNameInvalidLength(Language language, int minNameLength, int maxNameLength) {
        final var params = new HashMap<String, Object>();
        params.put("max_name_length", maxNameLength);
        params.put("min_name_length", minNameLength);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ChangeNameResource::personageNameInvalidLength),
            params
        );
    }

    public static String personageNameInvalidSymbols(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::personageNameInvalidSymbols);
    }

    public static String successNameChange(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::successNameChange);
    }

    public static String internalError(Language language) {
        return resources.getOrDefault(language, ChangeNameResource::internalError);
    }
}
