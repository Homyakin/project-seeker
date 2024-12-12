package ru.homyakin.seeker.locale;

import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.top.models.GroupTopPosition;
import ru.homyakin.seeker.game.top.models.PersonageTopPosition;
import ru.homyakin.seeker.infrastructure.Icons;

public class LocaleUtils {
    public static String groupNameWithBadge(Group group) {
        return Icons.STANDARD_GROUP_BADGE + group.name();
    }

    public static String groupNameWithBadge(GroupTopPosition position) {
        return Icons.STANDARD_GROUP_BADGE + position.name();
    }

    public static String personageNameWithBadge(PersonageTopPosition position) {
        return position.personageBadgeWithName();
    }
}
