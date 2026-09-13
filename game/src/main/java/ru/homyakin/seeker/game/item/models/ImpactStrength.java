package ru.homyakin.seeker.game.item.models;

import java.util.List;

public final class ImpactStrength {
    public static final int BASE_VALUE = 80;
    public static final int MAX_VALUE = 100;

    private ImpactStrength() {
    }

    public static int fromItems(List<Item> items) {
        long contribution = 0;
        for (final var item : items) {
            if (item.impact() < 0) {
                throw new IllegalArgumentException("Impact contribution must be non-negative: " + item.impact());
            }
            contribution = Math.addExact(contribution, item.impact());
        }
        return (int) Math.min(MAX_VALUE, Math.addExact(BASE_VALUE, contribution));
    }
}
