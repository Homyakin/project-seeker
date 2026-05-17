package ru.homyakin.seeker.game.item.errors;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public sealed interface LegacyPutOnItemError {
    enum PersonageMissingItem implements LegacyPutOnItemError { INSTANCE }

    enum AlreadyEquipped implements LegacyPutOnItemError { INSTANCE }

    record RequiredFreeSlots(List<PersonageSlot> slots) implements LegacyPutOnItemError {}
}
