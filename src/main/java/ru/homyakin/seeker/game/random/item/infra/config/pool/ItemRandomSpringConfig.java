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
    private final ProbabilityPicker<ItemRarity> worldRaidRarityPicker;

    public ItemRandomSpringConfig() {
        final var shopRarityProbabilities = new HashMap<ItemRarity, Integer>();
        shopRarityProbabilities.put(ItemRarity.COMMON, 44);
        shopRarityProbabilities.put(ItemRarity.UNCOMMON, 24);
        shopRarityProbabilities.put(ItemRarity.RARE, 14);
        shopRarityProbabilities.put(ItemRarity.EPIC, 11);
        shopRarityProbabilities.put(ItemRarity.LEGENDARY, 7);
        shopRarityPicker = new ProbabilityPicker<>(shopRarityProbabilities);

        final var worldRaidRarityProbabilities = new HashMap<ItemRarity, Integer>();
        worldRaidRarityProbabilities.put(ItemRarity.COMMON, 5);
        worldRaidRarityProbabilities.put(ItemRarity.UNCOMMON, 15);
        worldRaidRarityProbabilities.put(ItemRarity.RARE, 50);
        worldRaidRarityProbabilities.put(ItemRarity.EPIC, 20);
        worldRaidRarityProbabilities.put(ItemRarity.LEGENDARY, 10);
        worldRaidRarityPicker = new ProbabilityPicker<>(worldRaidRarityProbabilities);
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
    public ProbabilityPicker<ItemRarity> worldRaidRarityPicker() {
        return worldRaidRarityPicker;
    }
}
