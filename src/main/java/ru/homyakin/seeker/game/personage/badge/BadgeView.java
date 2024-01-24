package ru.homyakin.seeker.game.personage.badge;

import ru.homyakin.seeker.infrastructure.Icons;

public enum BadgeView {
    STANDARD("standard", Icons.PERSONAGE_STANDARD),
    FIRST_PERSONAGES("first-personages", Icons.PERSONAGE_FIRST),
    ;

    private final String code;
    private final String icon;

    BadgeView(String code, String icon) {
        this.code = code;
        this.icon = icon;
    }

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
