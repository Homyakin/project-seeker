package ru.homyakin.seeker.game.item;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public enum ItemType {
    ONE_HANDED_SWORD(List.of(PersonageSlot.MAIN_HAND)),
    POLE_WEAPON(List.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND)), // древковое
    ONE_HANDED_BLUNT(List.of(PersonageSlot.MAIN_HAND)) // дробящее
    ;

    private final List<PersonageSlot> slots;

    ItemType(List<PersonageSlot> slots) {
        this.slots = slots;
    }
}
