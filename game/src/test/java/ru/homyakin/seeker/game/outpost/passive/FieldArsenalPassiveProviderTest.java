package ru.homyakin.seeker.game.outpost.passive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.group.passive.GroupBuildingPassiveEffect;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;

class FieldArsenalPassiveProviderTest {
    private final FieldArsenalPassiveProvider provider = new FieldArsenalPassiveProvider();

    @Test
    void returnsEmptyWhenLevelIsZero() {
        final var config = config(1, 5);

        final var effects = provider.passiveEffects(0, config);

        Assertions.assertTrue(effects.isEmpty());
    }

    @Test
    void returnsLoadoutsAndBagSpaceScaledByLevel() {
        final var config = config(1, 5);

        final var effects = provider.passiveEffects(3, config);

        Assertions.assertEquals(2, effects.size());
        Assertions.assertEquals(Building.FIELD_ARSENAL, provider.building());

        final var loadouts = (GroupBuildingPassiveEffect) effects.getFirst();
        final var bagSpace = (GroupBuildingPassiveEffect) effects.get(1);
        Assertions.assertEquals(new Effect.ExtraLoadouts(3), loadouts.effect());
        Assertions.assertEquals(new Effect.ExtraBagSpace(15), bagSpace.effect());
        Assertions.assertTrue(loadouts.expiresAt().isEmpty());
        Assertions.assertTrue(bagSpace.expiresAt().isEmpty());
    }

    private static OutpostBuildingConfig config(int loadoutsPerLevel, int bagSpacePerLevel) {
        final var config = new OutpostBuildingConfig();
        config.setFieldArsenalLoadoutsPerLevel(loadoutsPerLevel);
        config.setFieldArsenalBagSpacePerLevel(bagSpacePerLevel);
        return config;
    }
}
