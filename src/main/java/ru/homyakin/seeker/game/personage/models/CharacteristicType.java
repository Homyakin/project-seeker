package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.infrastructure.TextConstants;

public enum CharacteristicType {
    STRENGTH(TextConstants.STRENGTH_ICON),
    AGILITY(TextConstants.AGILITY_ICON),
    WISDOM(TextConstants.WISDOM_ICON);

    private final String icon;

    CharacteristicType(String icon) {
        this.icon = icon;
    }

    public String icon() {
        return icon;
    }

    public static CharacteristicType findForce(String type) {
        for (CharacteristicType characteristicType : CharacteristicType.values()) {
            if (characteristicType.name().equals(type)) {
                return characteristicType;
            }
        }
        throw new IllegalStateException("Unknown characteristic type " + type);
    }
}
