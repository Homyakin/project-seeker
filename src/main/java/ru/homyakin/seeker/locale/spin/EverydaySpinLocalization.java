package ru.homyakin.seeker.locale.spin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.PersonageMention;
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
            CommonUtils.ifNullThen(map.get(language).notEnoughUsers(), map.get(Language.DEFAULT).notEnoughUsers()),
            Collections.singletonMap("required_users", requiredUsers)
        );
    }

    public static String alreadyChosen(Language language, PersonageMention mention) {
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThen(map.get(language).alreadyChosen(), map.get(Language.DEFAULT).alreadyChosen())
            ),
            Collections.singletonMap("mention_personage_badge_with_name", mention.value())
        );
    }

    public static String chosenUser(Language language, PersonageMention mention, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("mention_personage_badge_with_name", mention.value());
        params.put("money_icon", Icons.MONEY);
        params.put("money_count", money.value());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThen(map.get(language).chosenUser(), map.get(Language.DEFAULT).chosenUser())
            ),
            params
        );
    }

    public static String noChosenUsers(Language language) {
        return CommonUtils.ifNullThen(map.get(language).noChosenUsers(), map.get(Language.DEFAULT).noChosenUsers());
    }

    public static String topChosenUsers(Language language) {
        return CommonUtils.ifNullThen(map.get(language).topChosenUsers(), map.get(Language.DEFAULT).topChosenUsers());
    }
}
