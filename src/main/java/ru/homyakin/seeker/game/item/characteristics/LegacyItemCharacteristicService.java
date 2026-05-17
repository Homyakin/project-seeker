package ru.homyakin.seeker.game.item.characteristics;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.models.LegacyGenerateItemObject;
import ru.homyakin.seeker.game.item.modifier.models.LegacyGenerateModifier;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.List;

@Service
public class LegacyItemCharacteristicService {
    private final LegacyItemCharacteristicConfig config;

    public LegacyItemCharacteristicService(LegacyItemCharacteristicConfig config) {
        this.config = config;
    }

    public Characteristics createCharacteristics(
        LegacyItemRarity rarity, LegacyGenerateItemObject object, List<LegacyGenerateModifier> modifiers) {
        double attack = 0;
        double defense = 0;
        double health = 0;
        double multiplier = 1.0;

        final var objectTypes = object.characteristics().types();
        for (final var type: objectTypes) {
            switch (type) {
                case HEALTH -> health += (double) config.baseHealth() / objectTypes.size() * object.slots().size();
                case ATTACK -> attack += (double) config.baseAttack() / objectTypes.size() * object.slots().size();
                case DEFENSE -> defense += (double) config.baseDefense() / objectTypes.size() * object.slots().size();
            }
        }

        for (final var modifier: modifiers) {
            final var modifierTypes = modifier.characteristics().types();
            final var modifierImpact = config.modifierImpact();
            for (final var type: modifierTypes) {
                switch (type) {
                    case HEALTH -> health += calcModifierCharacteristic(config.baseHealth(), modifierTypes.size(), modifierImpact);
                    case ATTACK -> attack += calcModifierCharacteristic(config.baseAttack(), modifierTypes.size(), modifierImpact);
                    case DEFENSE -> defense += calcModifierCharacteristic(config.baseDefense(), modifierTypes.size(), modifierImpact);
                    case MULTIPLIER -> multiplier += modifierImpact / modifierTypes.size();
                }
            }
        }

        return new Characteristics(
            /*health*/ (int) Math.round(resultCharacteristic(health, multiplier) * rarity.multiplier()),
            /*attack*/ (int) Math.round(resultCharacteristic(attack, multiplier) * rarity.multiplier()),
            /*defense*/ (int) Math.round(resultCharacteristic(defense, multiplier) * rarity.multiplier())
        );
    }

    private double resultCharacteristic(double baseCharacteristic, double multiplier) {
        final double result;
        if (baseCharacteristic == 0 || multiplier == 1.0) {
            result = baseCharacteristic;
        } else {
            result = Math.max(baseCharacteristic + 1, baseCharacteristic * multiplier);
        }
        return RandomUtils.getCharacteristicWithDeviation(result, config.deviation());
    }

    private double calcModifierCharacteristic(Integer value, int typesCount, double modifierImpact) {
        return Math.max((double) value / typesCount * modifierImpact, 1);
    }
}
