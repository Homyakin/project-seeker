package ru.homyakin.seeker.game.random.item.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.random.item.entity.ModifierPoolSettings;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "homyakin.seeker.random.item")
public class ItemRandomSpringConfig implements ItemRandomConfig {
    private List<Map<ItemRarity, Integer>> rarityPools;
    private int sameSlotsInPool;
    private List<ModifierPoolSettings> modifierPoolSettings;

    @Override
    public ModifierPoolSettings modifierPoolSettings() {
        return RandomUtils.getRandomElement(modifierPoolSettings);
    }

    @Override
    public int sameSlotsInPool() {
        return sameSlotsInPool;
    }

    @Override
    public Map<ItemRarity, Integer> raritiesInPool() {
        return RandomUtils.getRandomElement(rarityPools);
    }

    public void setSameSlotsInPool(int sameSlotsInPool) {
        this.sameSlotsInPool = sameSlotsInPool;
    }

    public void setRarity(Map<String, String> rarity) {
        // Собираем сначала всё в мапу где для каждой редкости свой пул
        final var rarityValuesMap = new HashMap<ItemRarity, List<Integer>>();
        int poolCount = -1;
        for (final var entry : rarity.entrySet()) {
            final var rarityKey = toRarity(entry.getKey());
            final var intValues = Arrays.stream(entry.getValue().split(";"))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            if (poolCount == -1) {
                poolCount = intValues.size();
            } else if (poolCount != intValues.size()) {
                throw new IllegalArgumentException("All rarities pools must have the same size");
            }
            rarityValuesMap.put(rarityKey, intValues);
        }

        // Здесь уже собираем пулы из редкостей
        final var list = new ArrayList<Map<ItemRarity, Integer>>(poolCount);
        for (int i = 0; i < poolCount; i++) {
            final var map = new HashMap<ItemRarity, Integer>();
            for (final var entry : rarityValuesMap.entrySet()) {
                final var key = entry.getKey();
                final var value = entry.getValue().get(i);
                map.put(key, value);
            }
            list.add(map);
        }
        rarityPools = list;
    }

    public void setModifier(Map<String, String> rarity) {
        // Собираем сначала всё в мапу где для каждого количества модификаторов свой пул
        final var rarityValuesMap = new HashMap<Integer, List<Integer>>();
        int poolCount = -1;
        for (final var entry : rarity.entrySet()) {
            final var modifierKey = Integer.valueOf(entry.getKey());
            final var intValues = Arrays.stream(entry.getValue().split(";"))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            if (poolCount == -1) {
                poolCount = intValues.size();
            } else if (poolCount != intValues.size()) {
                throw new IllegalArgumentException("All rarities pools must have the same size");
            }
            rarityValuesMap.put(modifierKey, intValues);
        }

        // Здесь уже собираем пулы из модификаторов
        final var list = new ArrayList<ModifierPoolSettings>(poolCount);
        for (int i = 0; i < poolCount; i++) {
            final var settings = new ModifierPoolSettings(
                rarityValuesMap.get(0).get(i),
                rarityValuesMap.get(1).get(i),
                rarityValuesMap.get(2).get(i)
            );
            list.add(settings);
        }
        modifierPoolSettings = list;
    }

    private ItemRarity toRarity(String rarity) {
        return switch (rarity) {
            case "common" -> ItemRarity.COMMON;
            case "uncommon" -> ItemRarity.UNCOMMON;
            case "rare" -> ItemRarity.RARE;
            case "epic" -> ItemRarity.EPIC;
            case "legendary" -> ItemRarity.LEGENDARY;
            default -> throw new IllegalArgumentException("Unknown rarity: " + rarity);
        };
    }
}
