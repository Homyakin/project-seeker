package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.utils.ProbabilityPicker;

public interface ItemRandomConfig {
    int sameSlotsInPool();

    ProbabilityPicker<LegacyItemRarity> shopRarityPicker();

    ProbabilityPicker<Integer> shopModifierCountPicker();

    ProbabilityPicker<Integer> raidModifierCountPicker();

    ProbabilityPicker<LegacyItemRarity> worldRaidRarityPicker();

    ProbabilityPicker<Integer> worldRaidModifierCountPicker();
}
