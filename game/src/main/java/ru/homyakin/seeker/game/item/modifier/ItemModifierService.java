package ru.homyakin.seeker.game.item.modifier;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.models.CatalogModifier;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

@Service
public class ItemModifierService {
    private final ItemModifierDao itemModifierDao;

    public ItemModifierService(ItemModifierDao itemModifierDao) {
        this.itemModifierDao = itemModifierDao;
    }

    public Optional<CatalogModifier> pickModifier(ItemRarity rarity, ItemObject object, PersonageSlot slot) {
        if (rarity == ItemRarity.COMMON) {
            return Optional.empty();
        }
        return Optional.of(pickModifier(object, slot));
    }

    public CatalogModifier pickModifier(ItemObject object, PersonageSlot slot) {
        return itemModifierDao.getRandomModifier(slot, ModifierCompatibility.compatibleTypes(object));
    }
}
