package ru.homyakin.seeker.game.item.modifier;

import java.util.EnumSet;
import java.util.Set;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

/** Shared compatibility rule for modifier selection and catalog validation. */
public final class ModifierCompatibility {
    private ModifierCompatibility() {
    }

    public static Set<ModifierType> compatibleTypes(ItemObject object) {
        if (object == null) {
            throw new IllegalArgumentException("Item object must be specified");
        }
        final var types = EnumSet.of(ModifierType.ANY);
        if (!object.attacks().isEmpty()) {
            types.add(ModifierType.ATTACK);
        }
        if (object.defense().isPresent()) {
            types.add(ModifierType.DEFENSE);
        }
        return Set.copyOf(types);
    }

    public static boolean isCompatible(ItemObject object, Modifier modifier, PersonageSlot selectedSlot) {
        if (modifier == null) {
            throw new IllegalArgumentException("Modifier must be specified");
        }
        if (selectedSlot == null) {
            throw new IllegalArgumentException("Selected slot must be specified");
        }
        return object.slots().contains(selectedSlot)
            && modifier.availableOnSlots().contains(selectedSlot)
            && compatibleTypes(object).contains(modifier.type());
    }
}
