package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ChangeNameLocalization {
    private static final Map<Language, ChangeNameResource> map = new HashMap<>();

    public static void add(Language language, ChangeNameResource resource) {
        map.put(language, resource);
    }

    public static String changeNameWithoutName(Language language) {
        return CommonUtils.ifNullThan(
            map.get(language).changeNameWithoutName(), map.get(Language.DEFAULT).changeNameWithoutName()
        );
    }

    public static String initChangeName(Language language) {
        return CommonUtils.ifNullThan(map.get(language).initChangeName(), map.get(Language.DEFAULT).initChangeName());
    }

    public static String cancelChangeName(Language language) {
        return CommonUtils.ifNullThan(map.get(language).cancelChangeName(), map.get(Language.DEFAULT).cancelChangeName());
    }

    public static String confirmName(Language language, String name) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).confirmName(), map.get(Language.DEFAULT).confirmName()),
            Collections.singletonMap("name", name)
        );
    }

    public static String cancelButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).cancelButton(), map.get(Language.DEFAULT).cancelButton());
    }

    public static String confirmButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).confirmButton(), map.get(Language.DEFAULT).confirmButton());
    }

    public static String repeatButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).repeatButton(), map.get(Language.DEFAULT).repeatButton());
    }

    public static String personageNameInvalidLength(Language language, int minNameLength, int maxNameLength) {
        final var params = new HashMap<String, Object>();
        params.put("max_name_length", maxNameLength);
        params.put("min_name_length", minNameLength);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).personageNameInvalidLength(), map.get(Language.DEFAULT).personageNameInvalidLength()),
            params
        );
    }

    public static String personageNameInvalidSymbols(Language language) {
        return CommonUtils.ifNullThan(
            map.get(language).personageNameInvalidSymbols(), map.get(Language.DEFAULT).personageNameInvalidSymbols()
        );
    }

    public static String successNameChange(Language language) {
        return CommonUtils.ifNullThan(map.get(language).successNameChange(), map.get(Language.DEFAULT).successNameChange());
    }

    public static String internalError(Language language) {
        return CommonUtils.ifNullThan(map.get(language).internalError(), map.get(Language.DEFAULT).internalError());
    }
}
