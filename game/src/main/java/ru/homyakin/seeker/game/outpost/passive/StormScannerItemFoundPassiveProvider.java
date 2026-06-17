package ru.homyakin.seeker.game.outpost.passive;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.group.passive.GroupBuildingPassiveEffect;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;

@Component
public class StormScannerItemFoundPassiveProvider implements BuildingPassiveEffectProvider {
    @Override
    public Building building() {
        return Building.STORM_SCANNER;
    }

    @Override
    public Optional<GroupPassiveEffect> passiveEffect(int completedLevel, OutpostBuildingConfig config) {
        if (completedLevel <= 0) {
            return Optional.empty();
        }
        final int perLevel = config.getStormScannerItemFoundPercentPerLevel();
        final int totalPercent = completedLevel * perLevel;
        if (totalPercent <= 0) {
            return Optional.empty();
        }
        return Optional.of(
            new GroupBuildingPassiveEffect(
                Building.STORM_SCANNER,
                new Effect.ItemFoundChancePercent(totalPercent),
                Optional.empty()
            )
        );
    }
}
