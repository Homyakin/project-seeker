package ru.homyakin.seeker.locale;

import ru.homyakin.seeker.game.online.entity.OnlineType;

public final class GroupMembersOnlineIndicator {
    private GroupMembersOnlineIndicator() {
    }

    /**
     * Четыре порога (по возрастанию) задают границы 🟢 → 🟡 → 🟠 → 🔴; старше последнего — ⚫️.
     */
    public static String emoji(OnlineType type) {
        return switch (type) {
            case ACTIVE -> "🟢";
            case NOT_ACTIVE_LEVEL_1 -> "🟡";
            case NOT_ACTIVE_LEVEL_2 -> "🟠";
            case NOT_ACTIVE_LEVEL_3 -> "🔴";
            case NOT_ACTIVE_LEVEL_4 -> "⚫️";
        };
    }
}
