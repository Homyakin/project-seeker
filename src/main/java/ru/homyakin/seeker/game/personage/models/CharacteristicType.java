package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.infrastructure.Icons;

public enum CharacteristicType {
    STRENGTH(Icons.STRENGTH),
    AGILITY(Icons.AGILITY),
    WISDOM(Icons.WISDOM);

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
