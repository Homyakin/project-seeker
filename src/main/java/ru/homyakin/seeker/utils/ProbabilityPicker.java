package ru.homyakin.seeker.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ProbabilityPicker<T> {
    private final List<T> items;
    private final List<Integer> cumulativeWeights;
    private final int totalWeight;

    public ProbabilityPicker(Map<T, Integer> weightMap) {
        items = new ArrayList<>(weightMap.size());
        cumulativeWeights = new ArrayList<>(weightMap.size());
        var cumulative = 0;
        for (Map.Entry<T, Integer> entry : weightMap.entrySet()) {
            items.add(entry.getKey());
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException("Weights must be non-negative");
            }
            cumulative += entry.getValue();
            cumulativeWeights.add(cumulative);
        }
        totalWeight = cumulative;
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Total weight must be positive");
        }
    }

    public T pick(Function<Integer, Integer> randomWithUpperBound) {
        final var value = randomWithUpperBound.apply(totalWeight);
        for (int i = 0; i < cumulativeWeights.size(); i++) {
            if (value < cumulativeWeights.get(i)) {
                return items.get(i);
            }
        }
        // Should never reach here
        return items.getLast();
    }

    public Map<T, Double> getProbabilities() {
        final var map = new HashMap<T, Double>();
        for (int i = 0; i < items.size(); i++) {
            int weight;
            if (i == 0) {
                weight = cumulativeWeights.get(i);
            } else {
                weight = cumulativeWeights.get(i) - cumulativeWeights.get(i - 1);
            }
            double probability = (double) weight / totalWeight * 100;
            map.put(items.get(i), probability);
        }
        return map;
    }
}
