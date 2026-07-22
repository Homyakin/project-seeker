package ru.homyakin.seeker.game.event.anomaly.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.utils.RandomUtils;

public enum AnomalyModifier {
    STORM_SURGE("storm-surge", new Effect.Multiplier(15, EffectCharacteristic.ATTACK)),
    CRYSTAL_SHELL("crystal-shell", new Effect.Multiplier(15, EffectCharacteristic.HEALTH)),
    WEAK_SIGNAL("weak-signal", new Effect.MinusMultiplier(10, EffectCharacteristic.ATTACK)),
    THIN_AIR("thin-air", new Effect.MinusMultiplier(10, EffectCharacteristic.HEALTH)),
    ;

    private final String code;
    private final Effect effect;

    AnomalyModifier(String code, Effect effect) {
        this.code = code;
        this.effect = effect;
    }

    public String code() {
        return code;
    }

    public Effect effect() {
        return effect;
    }

    public static AnomalyModifier random() {
        final var values = values();
        return values[RandomUtils.getInInterval(0, values.length - 1)];
    }

    public static Optional<AnomalyModifier> findByCode(String code) {
        return Arrays.stream(values())
            .filter(it -> it.code.equals(code))
            .findFirst();
    }

    public static List<AnomalyModifier> all() {
        return List.of(values());
    }
}
