package ru.homyakin.seeker.game.item.modifier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao.ModifierRow;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

@Service
public class ItemModifierService {
    private final ItemModifierDao itemModifierDao;

    public ItemModifierService(ItemModifierDao itemModifierDao) {
        this.itemModifierDao = itemModifierDao;
    }

    public Optional<ModifierRow> pickModifier(ItemRarity rarity, ItemObject object, PersonageSlot slot) {
        if (rarity == ItemRarity.COMMON) {
            return Optional.empty();
        }
        return Optional.of(itemModifierDao.getRandomModifier(slot, compatibleModifierTypes(object)));
    }

    private Set<ModifierType> compatibleModifierTypes(ItemObject object) {
        final var types = new HashSet<ModifierType>();
        types.add(ModifierType.ANY);
        if (object.attack().isPresent()) {
            types.add(ModifierType.ATTACK);
        }
        if (object.defense().isPresent()) {
            types.add(ModifierType.DEFENSE);
        }
        return types;
    }
}
