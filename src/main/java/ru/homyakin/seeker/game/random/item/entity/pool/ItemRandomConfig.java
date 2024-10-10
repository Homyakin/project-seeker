package ru.homyakin.seeker.game.random.item.entity.pool;

import ru.homyakin.seeker.game.item.models.ItemRarity;

import java.util.Map;

public interface ItemRandomConfig {
    ModifierPoolSettings modifierPoolSettings();

    int sameSlotsInPool();

    Map<ItemRarity, Integer> raritiesInPool();
}
