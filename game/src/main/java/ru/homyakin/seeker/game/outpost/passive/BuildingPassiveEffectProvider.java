package ru.homyakin.seeker.game.outpost.passive;

import java.util.Optional;

import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;

public interface BuildingPassiveEffectProvider {
    Building building();

    Optional<GroupPassiveEffect> passiveEffect(int completedLevel, OutpostBuildingConfig config);
}
