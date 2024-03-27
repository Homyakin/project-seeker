package ru.homyakin.seeker.game.item.models;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public sealed interface PutOnItemError {
    enum PersonageMissingItem implements PutOnItemError { INSTANCE }

    enum AlreadyEquipped implements PutOnItemError { INSTANCE }

    record RequiredFreeSlots(List<PersonageSlot> slots) implements PutOnItemError {}
}
