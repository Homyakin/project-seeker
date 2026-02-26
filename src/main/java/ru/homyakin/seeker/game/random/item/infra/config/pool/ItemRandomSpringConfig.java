package ru.homyakin.seeker.game.random.item.infra.config.pool;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.utils.ProbabilityPicker;

import java.util.HashMap;

@Component
public class ItemRandomSpringConfig implements ItemRandomConfig {
    private final int sameSlotsInPool = 2;
    private final ProbabilityPicker<ItemRarity> shopRarityPicker;
    private final ProbabilityPicker<Integer> shopModifierCountPicker;
    private final ProbabilityPicker<Integer> raidModifierCountPicker;
    private final ProbabilityPicker<ItemRarity> worldRaidRarityPicker;
    private final ProbabilityPicker<Integer> worldRaidModifierCountPicker;

    public ItemRandomSpringConfig() {
        final var shopRarityProbabilities = new HashMap<ItemRarity, Integer>();
        shopRarityProbabilities.put(ItemRarity.COMMON, 44);
        shopRarityProbabilities.put(ItemRarity.UNCOMMON, 24);
        shopRarityProbabilities.put(ItemRarity.RARE, 14);
        shopRarityProbabilities.put(ItemRarity.EPIC, 11);
        shopRarityProbabilities.put(ItemRarity.LEGENDARY, 7);
        shopRarityPicker = new ProbabilityPicker<>(shopRarityProbabilities);

        final var shopModifierCountProbabilities = new HashMap<Integer, Integer>();
        shopModifierCountProbabilities.put(0, 55);
        shopModifierCountProbabilities.put(1, 30);
        shopModifierCountProbabilities.put(2, 15);
        shopModifierCountPicker = new ProbabilityPicker<>(shopModifierCountProbabilities);

        final var raidModifierCountProbabilities = new HashMap<Integer, Integer>();
        raidModifierCountProbabilities.put(0, 40);
        raidModifierCountProbabilities.put(1, 40);
        raidModifierCountProbabilities.put(2, 20);
        raidModifierCountPicker = new ProbabilityPicker<>(raidModifierCountProbabilities);

        final var worldRaidRarityProbabilities = new HashMap<ItemRarity, Integer>();
        worldRaidRarityProbabilities.put(ItemRarity.RARE, 5);
        worldRaidRarityProbabilities.put(ItemRarity.EPIC, 60);
        worldRaidRarityProbabilities.put(ItemRarity.LEGENDARY, 35);
        worldRaidRarityPicker = new ProbabilityPicker<>(worldRaidRarityProbabilities);

        final var worldRaidModifierCountProbabilities = new HashMap<Integer, Integer>();
        worldRaidModifierCountProbabilities.put(0, 20);
        worldRaidModifierCountProbabilities.put(1, 30);
        worldRaidModifierCountProbabilities.put(2, 50);
        worldRaidModifierCountPicker = new ProbabilityPicker<>(worldRaidModifierCountProbabilities);
    }

    @Override
    public int sameSlotsInPool() {
        return sameSlotsInPool;
    }

    @Override
    public ProbabilityPicker<ItemRarity> shopRarityPicker() {
        return shopRarityPicker;
    }

    @Override
    public ProbabilityPicker<Integer> shopModifierCountPicker() {
        return shopModifierCountPicker;
    }

    @Override
    public ProbabilityPicker<Integer> raidModifierCountPicker() {
        return raidModifierCountPicker;
    }

    @Override
    public ProbabilityPicker<ItemRarity> worldRaidRarityPicker() {
        return worldRaidRarityPicker;
    }

    @Override
    public ProbabilityPicker<Integer> worldRaidModifierCountPicker() {
        return worldRaidModifierCountPicker;
    }
}
