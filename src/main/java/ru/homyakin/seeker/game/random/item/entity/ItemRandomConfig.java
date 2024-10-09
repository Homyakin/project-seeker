package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.item.rarity.ItemRarity;

import java.util.Map;

public interface ItemRandomConfig {
    ModifierPoolSettings modifierPoolSettings();

    int sameSlotsInPool();

    Map<ItemRarity, Integer> raritiesInPool();
}
