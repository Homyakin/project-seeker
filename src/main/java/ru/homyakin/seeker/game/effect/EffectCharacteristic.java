package ru.homyakin.seeker.game.effect;

import ru.homyakin.seeker.infrastructure.Icons;

public enum EffectCharacteristic {
    ATTACK(Icons.ATTACK),
    HEALTH(Icons.HEALTH),
    STRENGTH(Icons.STRENGTH),
    AGILITY(Icons.AGILITY),
    WISDOM(Icons.WISDOM),
    ;

    private final String icon;

    EffectCharacteristic(String icon) {
        this.icon = icon;
    }

    public String icon() {
        return icon;
    }
}
