package ru.homyakin.seeker.game.item.modifier;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@ConfigurationProperties("homyakin.seeker.item.modifier")
public class ItemModifierConfig implements Validator {
    private int zeroProbability;
    private int oneProbability;
    private int twoProbability;

    public int zeroProbability() {
        return zeroProbability;
    }

    public int oneProbability() {
        return oneProbability;
    }

    public int twoProbability() {
        return twoProbability;
    }

    public void setZeroProbability(int zeroProbability) {
        this.zeroProbability = zeroProbability;
    }

    public void setOneProbability(int oneProbability) {
        this.oneProbability = oneProbability;
    }

    public void setTwoProbability(int twoProbability) {
        this.twoProbability = twoProbability;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return this.getClass().isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof ItemModifierConfig config) {
            final var probabilitySum = config.zeroProbability + config.oneProbability + config.twoProbability;
            if (probabilitySum != 100) {
                errors.reject(
                    "item.modifier.probability.error",
                    "Item modifier probability sum should be 100, but is " + probabilitySum
                );
            }
        }
    }
}
