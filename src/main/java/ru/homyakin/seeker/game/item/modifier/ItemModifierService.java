package ru.homyakin.seeker.game.item.modifier;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.modifier.models.GenerateModifier;
import ru.homyakin.seeker.game.item.modifier.models.ModifierType;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemModifiers;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemModifierService {
    private final ItemModifierDao itemModifierDao;

    public ItemModifierService(ItemModifierDao itemModifierDao) {
        this.itemModifierDao = itemModifierDao;
    }

    public List<GenerateModifier> generate(ItemRarity rarity, int modifiersCount) {
        return switch (modifiersCount) {
            case 0 -> List.of();
            case 1 -> List.of(itemModifierDao.getRandomModifier(rarity));
            case 2 -> {
                final var modifiers = new ArrayList<GenerateModifier>();
                final var modifier = itemModifierDao.getRandomModifier(rarity);
                modifiers.add(modifier);
                // Может быть либо 2 префиксных, либо 1 суффикс и 1 префикс
                if (modifier.type() == ModifierType.SUFFIX) {
                    modifiers.add(itemModifierDao.getRandomModifierWithType(ModifierType.PREFIX, rarity));
                } else {
                    modifiers.add(itemModifierDao.getRandomModifierExcludeId(modifier.id(), rarity));
                }
                yield modifiers;
            }
            default -> throw new IllegalArgumentException("There can be only 0-2 modifiers. Got " + modifiersCount);
        };
    }

    public void saveModifiers(ItemModifiers modifiers) {
        modifiers.modifier().forEach(itemModifierDao::saveModifier);
    }
}
