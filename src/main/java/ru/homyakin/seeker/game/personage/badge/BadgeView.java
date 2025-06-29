package ru.homyakin.seeker.game.personage.badge;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.fellbaum.jemoji.EmojiManager;

public enum BadgeView {
    STANDARD("standard", EmojiManager.getByAlias(":beginner:").orElseThrow().getFirst().getEmoji()),
    FIRST_PERSONAGES("first-personages", EmojiManager.getByAlias(":reminder_ribbon:").orElseThrow().getFirst().getEmoji()),
    FIRST_SEASON("first-season", EmojiManager.getByAlias(":otter:").orElseThrow().getFirst().getEmoji()),
    SECOND_SEASON("second-season", EmojiManager.getByAlias(":cat2:").orElseThrow().getFirst().getEmoji()),
    NEW_YEAR_2025("new-year-2025", EmojiManager.getByAlias(":dizzy:").orElseThrow().getFirst().getEmoji()),
    THIRD_SEASON("third-season", EmojiManager.getByAlias(":rat:").orElseThrow().getFirst().getEmoji()),
    FIRST_GROUPS("first-groups", EmojiManager.getByAlias(":rosette:").orElseThrow().getFirst().getEmoji()),
    ;

    private final String code;
    private final String icon;

    BadgeView(String code, String icon) {
        this.code = code;
        this.icon = icon;
    }

    @JsonCreator
    public static BadgeView findByCode(String code) {
        for (BadgeView badge : values()) {
            if (badge.code.equals(code)) {
                return badge;
            }
        }
        throw new IllegalArgumentException("Invalid BadgeView Code: " + code);
    }

    public String icon() {
        return icon;
    }

    public String code() {
        return code;
    }
}
