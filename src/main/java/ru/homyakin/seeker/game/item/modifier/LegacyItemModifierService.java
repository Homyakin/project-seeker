package ru.homyakin.seeker.game.item.modifier;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.modifier.models.LegacyAlreadyMaxModifiers;
import ru.homyakin.seeker.game.item.modifier.models.LegacyGenerateModifier;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifier;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifierType;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.LegacyItemModifiers;

import java.util.ArrayList;
import java.util.List;

@Service
public class LegacyItemModifierService {
    private final LegacyItemModifierDao itemModifierDao;

    public LegacyItemModifierService(LegacyItemModifierDao itemModifierDao) {
        this.itemModifierDao = itemModifierDao;
    }

    public List<LegacyGenerateModifier> generate(LegacyItemRarity rarity, int modifiersCount) {
        return switch (modifiersCount) {
            case 0 -> List.of();
            case 1 -> List.of(itemModifierDao.getRandomModifier(rarity));
            case 2 -> {
                final var modifiers = new ArrayList<LegacyGenerateModifier>();
                final var modifier = itemModifierDao.getRandomModifier(rarity);
                modifiers.add(modifier);
                // Может быть либо 2 префиксных, либо 1 суффикс и 1 префикс
                if (modifier.type() == LegacyModifierType.SUFFIX) {
                    modifiers.add(itemModifierDao.getRandomModifierWithType(LegacyModifierType.PREFIX, rarity));
                } else {
                    modifiers.add(itemModifierDao.getRandomModifierExcludeId(modifier.id(), rarity));
                }
                yield modifiers;
            }
            default -> throw new IllegalArgumentException("There can be only 0-2 modifiers. Got " + modifiersCount);
        };
    }

    public Either<LegacyAlreadyMaxModifiers, List<LegacyGenerateModifier>> addModifier(
        LegacyItemRarity rarity, List<LegacyModifier> currentModifiers) {
        final var count = currentModifiers.size();
        return switch (count) {
            case 0 -> Either.right(List.of(itemModifierDao.getRandomModifier(rarity)));
            case 1 -> {
                final var currentModifier = currentModifiers.getFirst();
                final var modifiers = new ArrayList<LegacyGenerateModifier>();
                modifiers.add(itemModifierDao.getById(currentModifier.id()));
                if (currentModifier.type() == LegacyModifierType.SUFFIX) {
                    modifiers.add(itemModifierDao.getRandomModifierWithType(LegacyModifierType.PREFIX, rarity));
                } else {
                    modifiers.add(itemModifierDao.getRandomModifierExcludeId(currentModifier.id(), rarity));
                }
                yield Either.right(modifiers);
            }
            default -> Either.left(LegacyAlreadyMaxModifiers.INSTANCE);
        };
    }

    public List<LegacyGenerateModifier> mapModifiers(List<LegacyModifier> modifiers) {
        if (modifiers.isEmpty()) {
            return List.of();
        }
        return itemModifierDao.getByIds(modifiers.stream().map(LegacyModifier::id).toList());
    }

    public void saveModifiers(LegacyItemModifiers modifiers) {
        modifiers.modifier().forEach(itemModifierDao::saveModifier);
    }
}
