package ru.homyakin.seeker.game.item.characteristics;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.models.GenerateItemObject;
import ru.homyakin.seeker.game.item.models.GenerateModifier;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.IntRange;

import java.util.List;

@Service
public class ItemCharacteristicService {
    private final ItemCharacteristicConfig config;

    public ItemCharacteristicService(ItemCharacteristicConfig config) {
        this.config = config;
    }

    public Characteristics createCharacteristics(ItemRarity rarity, GenerateItemObject object, List<GenerateModifier> modifiers) {
        double attack = 0;
        double defense = 0;
        double health = 0;
        double multiplier = 1.0;

        final var objectTypes = object.characteristics().types();
        for (final var type: objectTypes) {
            switch (type) {
                case HEALTH -> health += RandomUtils.getCharacteristic(config.baseHealth()) / objectTypes.size() * object.slots().size();
                case ATTACK -> attack += RandomUtils.getCharacteristic(config.baseAttack()) / objectTypes.size() * object.slots().size();
                case DEFENSE -> defense += RandomUtils.getCharacteristic(config.baseDefense()) / objectTypes.size() * object.slots().size();
            }
        }

        for (final var modifier: modifiers) {
            final var modifierTypes = modifier.characteristics().types();
            final var modifierImpact = RandomUtils.getCharacteristic(config.modifierImpact());
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
            /*defense*/ (int) Math.round(resultCharacteristic(defense, multiplier) * rarity.multiplier()),
            /*strength*/ 0,
            /*agility*/ 0,
            /*wisdom*/ 0
        );
    }

    private double resultCharacteristic(double baseCharacteristic, double multiplier) {
        if (baseCharacteristic == 0 || multiplier == 1.0) {
            return baseCharacteristic;
        }
        return Math.max(baseCharacteristic + 1, baseCharacteristic * multiplier);
    }

    private double calcModifierCharacteristic(IntRange range, int typesCount, double modifierImpact) {
        return Math.max(RandomUtils.getCharacteristic(range) / typesCount * modifierImpact, 1);
    }
}
