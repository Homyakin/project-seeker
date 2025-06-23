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

    public static String groupTagWithBadge(Group group) {
        return Icons.STANDARD_GROUP_BADGE + tagString(group.tag());
    }

    public static String groupNameWithBadge(GroupProfile profile) {
        return groupNameWithBadge(profile.tag(), profile.name());
    }

    private static String groupNameWithBadge(Optional<String> tag, String name) {
        var tagString = tagString(tag);
        if (!tagString.isEmpty()) {
            tagString += " ";
        }
        return Icons.STANDARD_GROUP_BADGE + tagString + name;
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
        var tagString = tagString(tag);
        if (!tagString.isEmpty()) {
            tagString += " ";
        }
        return badge.icon() + tagString + name;
    }

    private static String tagString(Optional<String> tag) {
        return tag.map(t -> "[" + t + "]").orElse("");
    }

    public static String enabledIcon(boolean enabled) {
        return enabled ? Icons.ENABLED : Icons.DISABLED;
    }

    public static int power(int power) {
        return power / 100; // делим на 100, так как слишком большие числа (32052 в дефолте)
    }
}
