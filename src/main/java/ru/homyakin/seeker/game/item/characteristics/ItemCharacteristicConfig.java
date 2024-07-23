package ru.homyakin.seeker.game.item.characteristics;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("homyakin.seeker.item.characteristic")
public class ItemCharacteristicConfig {
    @NotNull
    private Integer baseAttack;
    @NotNull
    private Integer baseDefense;
    @NotNull
    private Integer baseHealth;
    @NotNull
    private Double modifierImpact;
    @NotNull
    private Double deviation;

    public Integer baseAttack() {
        return baseAttack;
    }

    public Integer baseDefense() {
        return baseDefense;
    }

    public Integer baseHealth() {
        return baseHealth;
    }

    public Double modifierImpact() {
        return modifierImpact;
    }

    public Double deviation() {
        return deviation;
    }

    public void setBaseAttack(Integer baseAttack) {
        this.baseAttack = baseAttack;
    }

    public void setBaseDefense(Integer baseDefense) {
        this.baseDefense = baseDefense;
    }

    public void setBaseHealth(Integer baseHealth) {
        this.baseHealth = baseHealth;
    }

    public void setModifierImpact(Double modifierImpact) {
        this.modifierImpact = modifierImpact;
    }

    public void setDeviation(Double deviation) {
        this.deviation = deviation;
    }
}
