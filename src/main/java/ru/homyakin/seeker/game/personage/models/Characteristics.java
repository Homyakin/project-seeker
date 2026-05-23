package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;

public record Characteristics(
    int health,
    int attack,
    int defense
) implements Cloneable {
    public Characteristics add(Characteristics other) {
        return new Characteristics(
            health + other.health,
            attack + other.attack,
            defense + other.defense
        );
    }

    public static Characteristics random() {
        return new Characteristics(500, 50, 20);
    }

    public Characteristics apply(PersonageEffects effects) {
        return new EditableCharacteristics(this).apply(effects).toFinal();
    }

    @Override
    public Characteristics clone() {
        try {
            return (Characteristics) super.clone();
        } catch (CloneNotSupportedException e) {
            //Не может быть в record
            throw new RuntimeException(e);
        }
    }

    private static class EditableCharacteristics {
        private int health;
        private int attack;
        private int defense;

        public EditableCharacteristics(Characteristics characteristics) {
            health = characteristics.health();
            attack = characteristics.attack();
            defense = characteristics.defense();
        }

        public EditableCharacteristics apply(PersonageEffects effects) {
            effects.effects().values().stream().map(PersonageEffect::effect).forEach(this::apply);
            return this;
        }

        private void apply(Effect effect) {
            switch (effect) {
                case Effect.Add add -> {
                    switch (add.characteristic()) {
                        case HEALTH -> health = health + add.value();
                        case ATTACK -> attack = attack + add.value();
                    }
                }
                case Effect.Multiplier multiplier -> {
                    final var value = 1 + multiplier.percent() / 100.0;
                    multiplyCharacteristic(value, multiplier.characteristic());
                }
                case Effect.MinusMultiplier multiplier -> {
                    final var value = 1 - multiplier.percent() / 100.0;
                    multiplyCharacteristic(value, multiplier.characteristic());
                }
                case Effect.RaidGoldRewardPercent _ -> {
                    // Group / meta effects: not applied to characteristics.
                }
                case Effect.ItemFoundChancePercent _ -> {
                    // Group / meta effects: not applied to characteristics.
                }
            }
        }

        private void multiplyCharacteristic(double value, EffectCharacteristic characteristic) {
            switch (characteristic) {
                case HEALTH -> health = (int) (health * value);
                case ATTACK -> attack = (int) (attack * value);
            }
        }

        public Characteristics toFinal() {
            return new Characteristics(
                health,
                attack,
                defense
            );
        }
    }

    public static final Characteristics ZERO = new Characteristics(0, 0, 0);
}
