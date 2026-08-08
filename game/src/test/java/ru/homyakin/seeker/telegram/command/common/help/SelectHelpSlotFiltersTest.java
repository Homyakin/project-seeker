package ru.homyakin.seeker.telegram.command.common.help;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public class SelectHelpSlotFiltersTest {
    @Test
    void encodesAndParsesSlotIds() {
        final var filters = Set.of(PersonageSlot.BODY, PersonageSlot.MAIN_HAND);
        final var encoded = SelectHelp.encodeSlotFilters(filters);
        Assertions.assertEquals("1,3", encoded);
        Assertions.assertEquals(filters, SelectHelp.parseSlotFilters(encoded));
    }

    @Test
    void emptyFiltersUseDash() {
        Assertions.assertEquals(SelectHelp.EMPTY_SLOT_FILTERS, SelectHelp.encodeSlotFilters(Set.of()));
        Assertions.assertEquals(Set.of(), SelectHelp.parseSlotFilters(SelectHelp.EMPTY_SLOT_FILTERS));
        Assertions.assertEquals(Set.of(), SelectHelp.parseSlotFilters(""));
    }
}
