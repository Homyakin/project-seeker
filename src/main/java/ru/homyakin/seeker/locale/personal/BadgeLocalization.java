package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.homyakin.seeker.game.personage.badge.Badge;
import ru.homyakin.seeker.game.personage.badge.PersonageAvailableBadge;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class BadgeLocalization {
    private static final Map<Language, BadgeResource> map = new HashMap<>();

    public static void add(Language language, BadgeResource resource) {
        map.put(language, resource);
    }

    public static String availableBadges(Language language, List<PersonageAvailableBadge> badges) {
        final var showBadges = badges.stream()
            .map(badge -> badge(language, badge.badge()))
            .toList();
        final var badgeList = String.join("\n", showBadges);
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).availableBadges(), map.get(Language.DEFAULT).availableBadges()),
            Collections.singletonMap("badge_list", badgeList)
        );
    }

    private static String badge(Language language, Badge badge) {
        final var badgeLocale = LocaleUtils.getLocaleByLanguageOrDefault(badge.locales(), language);
        final var params = new HashMap<String, Object>();
        params.put("badge_icon", badge.view().icon());
        params.put("badge_description", badgeLocale.orElseThrow().description());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).badge(), map.get(Language.DEFAULT).badge()),
            params
        );
    }

    public static String badgeIsNotAvailable(Language language) {
        return CommonUtils.ifNullThen(map.get(language).badgeIsNotAvailable(), map.get(Language.DEFAULT).badgeIsNotAvailable());
    }
}
