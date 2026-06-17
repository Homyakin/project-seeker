package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;

import java.util.Objects;

public final class Characteristics {
    private int health;
    private int attack;
    private int defense;

    public Characteristics(int health, int attack, int defense) {
        this.health = health;
        this.attack = attack;
        this.defense = defense;
    }

    public Characteristics(Characteristics other) {
        this(other.health, other.attack, other.defense);
    }

    public int health() {
        return health;
    }

    public int attack() {
        return attack;
    }

    public int defense() {
        return defense;
    }

    public Characteristics add(Characteristics other) {
        health += other.health;
        attack += other.attack;
        defense += other.defense;
        return this;
    }

    public Characteristics apply(PersonageEffects effects) {
        final var result = new Characteristics(this);
        result.applyEffects(effects);
        return result;
    }

    private void applyEffects(PersonageEffects effects) {
        effects.effects().values().stream().map(PersonageEffect::effect).forEach(this::applyEffect);
    }

    private void applyEffect(Effect effect) {
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
            case Effect.RaidGoldRewardPercent _, Effect.ItemFoundChancePercent _ -> {
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

    public static Characteristics empty() {
        return new Characteristics(0, 0, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Characteristics other) {
            return health == other.health
                && attack == other.attack
                && defense == other.defense;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(health, attack, defense);
    }
}
