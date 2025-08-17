package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.utils.ProbabilityPicker;

public interface ItemRandomConfig {
    int sameSlotsInPool();

    ProbabilityPicker<ItemRarity> shopRarityPicker();

    ProbabilityPicker<Integer> shopModifierCountPicker();

    ProbabilityPicker<Integer> raidModifierCountPicker();

    ProbabilityPicker<ItemRarity> worldRaidRarityPicker();

    ProbabilityPicker<Integer> worldRaidModifierCountPicker();
}
