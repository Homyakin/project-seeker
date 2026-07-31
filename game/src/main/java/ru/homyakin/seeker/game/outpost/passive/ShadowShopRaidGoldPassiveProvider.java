package ru.homyakin.seeker.game.outpost.passive;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.group.passive.GroupBuildingPassiveEffect;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;

@Component
public class ShadowShopRaidGoldPassiveProvider implements BuildingPassiveEffectProvider {
    @Override
    public Building building() {
        return Building.SHADOW_SHOP;
    }

    @Override
    public List<GroupPassiveEffect> passiveEffects(int completedLevel, OutpostBuildingConfig config) {
        if (completedLevel <= 0) {
            return List.of();
        }
        final int perLevel = config.getShadowShopRaidGoldPercentPerLevel();
        final int totalPercent = completedLevel * perLevel;
        if (totalPercent <= 0) {
            return List.of();
        }
        return List.of(
            new GroupBuildingPassiveEffect(
                Building.SHADOW_SHOP,
                new Effect.RaidGoldRewardPercent(totalPercent),
                Optional.empty()
            )
        );
    }
}
