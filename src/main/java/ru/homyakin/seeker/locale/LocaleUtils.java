package ru.homyakin.seeker.locale;

import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.infrastructure.Icons;

public class LocaleUtils {
    public static String groupNameWithBadge(Group group) {
        return Icons.STANDARD_GROUP_BADGE + group.name();
    }
}
