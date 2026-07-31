package ru.homyakin.seeker.game.outpost.passive;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.group.passive.GroupBuildingPassiveEffect;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;

@Component
public class FieldArsenalPassiveProvider implements BuildingPassiveEffectProvider {
    @Override
    public Building building() {
        return Building.FIELD_ARSENAL;
    }

    @Override
    public List<GroupPassiveEffect> passiveEffects(int completedLevel, OutpostBuildingConfig config) {
        if (completedLevel <= 0) {
            return List.of();
        }
        final var effects = new ArrayList<GroupPassiveEffect>();
        final int extraLoadouts = completedLevel * config.getFieldArsenalLoadoutsPerLevel();
        if (extraLoadouts > 0) {
            effects.add(
                new GroupBuildingPassiveEffect(
                    Building.FIELD_ARSENAL,
                    new Effect.ExtraLoadouts(extraLoadouts),
                    Optional.empty()
                )
            );
        }
        final int extraBagSpace = completedLevel * config.getFieldArsenalBagSpacePerLevel();
        if (extraBagSpace > 0) {
            effects.add(
                new GroupBuildingPassiveEffect(
                    Building.FIELD_ARSENAL,
                    new Effect.ExtraBagSpace(extraBagSpace),
                    Optional.empty()
                )
            );
        }
        return effects;
    }
}
