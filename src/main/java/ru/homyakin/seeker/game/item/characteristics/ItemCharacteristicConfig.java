package ru.homyakin.seeker.game.item.characteristics;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import ru.homyakin.seeker.utils.models.DoubleRange;
import ru.homyakin.seeker.utils.models.IntRange;

@Validated
@ConfigurationProperties("homyakin.seeker.item.characteristic")
public class ItemCharacteristicConfig {
    @NotNull
    private IntRange baseAttack;
    @NotNull
    private IntRange baseDefense;
    @NotNull
    private IntRange baseHealth;
    @NotNull
    private DoubleRange modifierImpact;

    public IntRange baseAttack() {
        return baseAttack;
    }

    public IntRange baseDefense() {
        return baseDefense;
    }

    public IntRange baseHealth() {
        return baseHealth;
    }

    public DoubleRange modifierImpact() {
        return modifierImpact;
    }

    public void setBaseAttack(String baseAttack) {
        this.baseAttack = IntRange.fromString(baseAttack);
    }

    public void setBaseDefense(String baseDefense) {
        this.baseDefense = IntRange.fromString(baseDefense);
    }

    public void setBaseHealth(String baseHealth) {
        this.baseHealth = IntRange.fromString(baseHealth);
    }

    public void setModifierImpact(String modifierImpact) {
        this.modifierImpact = DoubleRange.fromString(modifierImpact);
    }
}
