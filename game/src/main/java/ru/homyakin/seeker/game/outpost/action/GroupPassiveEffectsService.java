package ru.homyakin.seeker.game.outpost.action;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.effect.ItemFoundChanceBonus;
import ru.homyakin.seeker.game.effect.RaidGoldRewardBonus;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.game.outpost.passive.BuildingPassiveEffectProvider;

@Component
public class GroupPassiveEffectsService {
    private final OutpostStorage outpostStorage;
    private final OutpostBuildingConfig outpostBuildingConfig;
    private final SyncGroupTaxCommand syncGroupTaxCommand;
    private final List<BuildingPassiveEffectProvider> buildingPassiveEffectProviders;

    public GroupPassiveEffectsService(
        OutpostStorage outpostStorage,
        OutpostBuildingConfig outpostBuildingConfig,
        SyncGroupTaxCommand syncGroupTaxCommand,
        List<BuildingPassiveEffectProvider> buildingPassiveEffectProviders
    ) {
        this.outpostStorage = outpostStorage;
        this.outpostBuildingConfig = outpostBuildingConfig;
        this.syncGroupTaxCommand = syncGroupTaxCommand;
        this.buildingPassiveEffectProviders = buildingPassiveEffectProviders;
        final var existingTypes = new HashSet<Building>();
        for (final var provider : buildingPassiveEffectProviders) {
            if (existingTypes.contains(provider.building())) {
                throw new IllegalStateException(
                    "Building passive effect provider already registered: "
                        + provider.building()
                );
            }
            existingTypes.add(provider.building());
        }
    }

    public List<GroupPassiveEffect> listPassiveEffects(GroupId groupId) {
        syncGroupTaxCommand.execute(groupId);
        final var levelByBuilding = new HashMap<Building, Integer>();
        for (final var slot : outpostStorage.listBuildingSlots(groupId)) {
            if (slot.level() != 0) {
                levelByBuilding.put(slot.building(), slot.level());
            }
        }
        final var effects = new ArrayList<GroupPassiveEffect>();
        for (final var provider : buildingPassiveEffectProviders) {
            final int level = levelByBuilding.getOrDefault(provider.building(), 0);
            if (level > 0) {
                provider.passiveEffect(level, outpostBuildingConfig).ifPresent(effects::add);
            }
        }
        return effects;
    }

    /**
     * Sum of {@link ru.homyakin.seeker.game.effect.Effect.RaidGoldRewardPercent} from group passives;
     * one {@link #listPassiveEffects} load per call.
     */
    public int raidGoldBonusPercentSum(GroupId groupId, LocalDateTime now) {
        return RaidGoldRewardBonus.sumGroupPassiveEffects(listPassiveEffects(groupId), now);
    }

    /**
     * Sum of {@link ru.homyakin.seeker.game.effect.Effect.ItemFoundChancePercent} from group passives;
     * one {@link #listPassiveEffects} load per call.
     */
    public int itemFoundChanceBonusPercentSum(GroupId groupId, LocalDateTime now) {
        return ItemFoundChanceBonus.sumGroupPassiveEffects(listPassiveEffects(groupId), now);
    }
}
