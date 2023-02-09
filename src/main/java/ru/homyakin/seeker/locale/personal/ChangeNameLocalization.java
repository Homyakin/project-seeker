package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ChangeNameLocalization {
    private static final Map<Language, ChangeNameResource> map = new HashMap<>();

    public static void add(Language language, ChangeNameResource resource) {
        map.put(language, resource);
    }

    public static String changeNameWithoutName(Language language) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).changeNameWithoutName(), map.get(Language.DEFAULT).changeNameWithoutName()),
            Collections.singletonMap("name_command", CommandType.CHANGE_NAME.getText())
        );
    }

    public static String personageNameInvalidLength(Language language, int minNameLength, int maxNameLength) {
        final var params = new HashMap<String, Object>() {{
            put("max_name_length", maxNameLength);
            put("min_name_length", minNameLength);
        }};
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
}
