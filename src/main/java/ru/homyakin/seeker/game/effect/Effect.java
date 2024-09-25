package ru.homyakin.seeker.game.effect;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public sealed interface Effect {
    record Multiplier(int percent, EffectCharacteristic characteristic) implements Effect {
    }

    record Add(int value, EffectCharacteristic characteristic) implements Effect {
    }

    record MinusMultiplier(int percent, EffectCharacteristic characteristic) implements Effect {
    }
}
