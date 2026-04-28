package ru.homyakin.seeker.game.battle.v4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;

class DuelSimulatorTest {
    private static final int DUELS_PER_ORDER = 10_000;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Test
    public void statistic() throws InterruptedException {
        final var templates = new DuelPersonageTemplate[] {
            DuelPersonageTemplate.BASE,
            DuelPersonageTemplate.COMMON,
            DuelPersonageTemplate.UNCOMMON,
            DuelPersonageTemplate.RARE,
            DuelPersonageTemplate.EPIC,
            DuelPersonageTemplate.LEGENDARY
        };
        final var pairs = new HashMap<DuelPersonageTemplate, List<DuelPersonageTemplate>>();
        for (int i = 0; i < templates.length; ++i) {
            final var opponents = new ArrayList<DuelPersonageTemplate>();
            for (int j = i; j < templates.length; ++j) {
                opponents.add(templates[j]);
            }
            pairs.put(templates[i], opponents);
        }
        for (final var entry : pairs.entrySet()) {
            for (final var opponent : pairs.get(entry.getKey())) {
                final var stats = DuelSimulator.simulatePair(entry.getKey(), opponent, DUELS_PER_ORDER);
                System.out.println(stats);
            }
        }
    }

    @Test
    public void lightHeavy() {
        final var stats = DuelSimulator.simulatePair(DuelPersonageTemplate.LEGENDARY_LIGHT, DuelPersonageTemplate.LEGENDARY_HEAVY, DUELS_PER_ORDER);
        System.out.println(stats);
    }

    @Test
    public void commonLeg() {
        var stats = DuelSimulator.simulatePair(DuelPersonageTemplate.COMMON_LIGHT, DuelPersonageTemplate.LEGENDARY_HEAVY, DUELS_PER_ORDER);
        System.out.println(stats);
        stats = DuelSimulator.simulatePair(DuelPersonageTemplate.COMMON_HEAVY, DuelPersonageTemplate.LEGENDARY_LIGHT, DUELS_PER_ORDER);
        System.out.println(stats);
    }
}
