package ru.homyakin.seeker.locale;

import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.top.models.GroupTopPosition;
import ru.homyakin.seeker.game.top.models.PersonageTopPosition;
import ru.homyakin.seeker.infrastructure.Icons;

import java.util.Optional;

public class LocaleUtils {
    public static String groupNameWithBadge(Group group) {
        return groupNameWithBadge(group.tag(), group.name());
    }

    public static String groupNameWithBadge(GroupTopPosition position) {
        return groupNameWithBadge(position.tag(), position.name());
    }

    public static String groupNameWithBadge(GroupProfile profile) {
        return groupNameWithBadge(profile.tag(), profile.name());
    }

    private static String groupNameWithBadge(Optional<String> tag, String name) {
        return Icons.STANDARD_GROUP_BADGE + tagString(tag) + name;
    }

    public static String personageNameWithBadge(PersonageTopPosition position) {
        return personageNameWithBadge(position.personageBadge(), position.tag(), position.personageName());
    }

    public static String personageNameWithBadge(Personage personage) {
        return personageNameWithBadge(personage.badge(), personage.tag(), personage.name());
    }

    private static String personageNameWithBadge(
        BadgeView badge,
        Optional<String> tag,
        String name
    ) {
        return badge.icon() + tagString(tag) + name;
    }

    private static String tagString(Optional<String> tag) {
        return tag.map(t -> "[" + t + "] ").orElse("");
    }
}
