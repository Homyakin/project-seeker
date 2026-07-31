package ru.homyakin.seeker.game.outpost.passive;

import java.util.List;

import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;

public interface BuildingPassiveEffectProvider {
    Building building();

    List<GroupPassiveEffect> passiveEffects(int completedLevel, OutpostBuildingConfig config);
}
