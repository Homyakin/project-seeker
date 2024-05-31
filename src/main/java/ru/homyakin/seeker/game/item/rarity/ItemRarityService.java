package ru.homyakin.seeker.game.item.rarity;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.utils.RandomUtils;

@Service
public class ItemRarityService {
    private final ItemRarityConfig config;

    public ItemRarityService(ItemRarityConfig config) {
        this.config = config;
    }

    public ItemRarity generateItemRarity() {
        int probability = RandomUtils.getInInterval(1, 100);
        if (probability <= config.legendaryProbability()) {
            return ItemRarity.LEGENDARY;
        } else if (probability <= config.legendaryProbability() + config.epicProbability()) {
            return ItemRarity.EPIC;
        } else if (probability <= config.legendaryProbability() + config.epicProbability() + config.rareProbability()) {
            return ItemRarity.RARE;
        } else if (probability <= config.legendaryProbability() + config.epicProbability() + config.rareProbability()
            + config.uncommonProbability()
        ) {
            return ItemRarity.UNCOMMON;
        } else {
            return ItemRarity.COMMON;
        }
    }
}
