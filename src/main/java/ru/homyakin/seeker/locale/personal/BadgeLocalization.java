package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import ru.homyakin.seeker.game.personage.badge.Badge;
import ru.homyakin.seeker.game.personage.badge.PersonageAvailableBadge;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class BadgeLocalization {
    private static final Resources<BadgeResource> resources = new Resources<>();

    public static void add(Language language, BadgeResource resource) {
        resources.add(language, resource);
    }

    public static String availableBadges(Language language, List<PersonageAvailableBadge> badges) {
        final var showBadges = badges.stream()
            .map(badge -> badge(language, badge.badge()))
            .toList();
        final var badgeList = String.join("\n", showBadges);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BadgeResource::availableBadges),
            Collections.singletonMap("badge_list", badgeList)
        );
    }

    private static String badge(Language language, Badge badge) {
        final var badgeLocale = badge.getLocaleOrDefault(language);
        final var params = new HashMap<String, Object>();
        params.put("badge_icon", badge.view().icon());
        params.put("badge_description", badgeLocale.description());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BadgeResource::badge),
            params
        );
    }

    public static String badgeIsNotAvailable(Language language) {
        return resources.getOrDefault(language, BadgeResource::badgeIsNotAvailable);
    }
}
