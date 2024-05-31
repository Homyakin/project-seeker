package ru.homyakin.seeker.game.item.rarity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Configuration
@ConfigurationProperties("homyakin.seeker.item.rarity")
public class ItemRarityConfig implements Validator {
    private int commonProbability;
    private int uncommonProbability;
    private int rareProbability;
    private int epicProbability;
    private int legendaryProbability;

    public int commonProbability() {
        return commonProbability;
    }

    public int uncommonProbability() {
        return uncommonProbability;
    }

    public int rareProbability() {
        return rareProbability;
    }

    public int epicProbability() {
        return epicProbability;
    }

    public int legendaryProbability() {
        return legendaryProbability;
    }

    public void setCommonProbability(int commonProbability) {
        this.commonProbability = commonProbability;
    }

    public void setUncommonProbability(int uncommonProbability) {
        this.uncommonProbability = uncommonProbability;
    }

    public void setRareProbability(int rareProbability) {
        this.rareProbability = rareProbability;
    }

    public void setEpicProbability(int epicProbability) {
        this.epicProbability = epicProbability;
    }

    public void setLegendaryProbability(int legendaryProbability) {
        this.legendaryProbability = legendaryProbability;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return this.getClass().isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof ItemRarityConfig config) {
            final var probabilitySum = config.commonProbability + config.uncommonProbability + config.rareProbability
                + config.epicProbability + config.legendaryProbability;
            if (probabilitySum != 100) {
                errors.reject(
                    "item.probability.error",
                    "Item probability sum should be 100, but is " + probabilitySum
                );
            }
        }
    }
}
