package ru.homyakin.seeker.game.outpost;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OutpostBuildingConfigSlotsTest {
    private final OutpostBuildingConfig config = new OutpostBuildingConfig();

    @Test
    void slotsIncreaseWithMonolithLevel() {
        Assertions.assertEquals(1, config.slotsByMonolithLevel(0));
        Assertions.assertEquals(2, config.slotsByMonolithLevel(1));
        Assertions.assertEquals(3, config.slotsByMonolithLevel(2));
        Assertions.assertEquals(4, config.slotsByMonolithLevel(3));
        Assertions.assertEquals(4, config.slotsByMonolithLevel(4));
        Assertions.assertEquals(4, config.slotsByMonolithLevel(5));
    }
}
