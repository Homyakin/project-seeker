package ru.homyakin.seeker.locale.spin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class EverydaySpinLocalization {
    private static final Map<Language, EverydaySpinResource> map = new HashMap<>();

    public static void add(Language language, EverydaySpinResource resource) {
        map.put(language, resource);
    }

    public static String notEnoughUsers(Language language, int requiredUsers) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).notEnoughUsers(), map.get(Language.DEFAULT).notEnoughUsers()),
            Collections.singletonMap("required_users", requiredUsers)
        );
    }

    public static String alreadyChosen(Language language, Personage personage) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).alreadyChosen(), map.get(Language.DEFAULT).alreadyChosen()),
            Collections.singletonMap("personage_icon_with_name", personage.iconWithName())
        );
    }

    public static String chosenUser(Language language, Personage personage) {
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(map.get(language).chosenUser(), map.get(Language.DEFAULT).chosenUser())
            ),
            Collections.singletonMap("personage_icon_with_name", personage.iconWithName())
        );
    }
}
