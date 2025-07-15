package ru.homyakin.seeker.game.battle.v4;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.utils.RandomUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.jfree.data.statistics.HistogramDataset;

public class BattleTest {
    private final RandomUtils randomUtils = mock();
    private final TwoPersonageTeamsBattle battle = new TwoPersonageTeamsBattle(randomUtils);

    @Test
    public void firstTeamFirst() {
        when(randomUtils.getInIntervalNotStatic(1, 2)).thenReturn(1);
        List<Double> winrates = calcFirstTeamWinrates();
        writeHistogramPng(
            "Процент побед (100хп, 10атк) 5 скорости VS 10 скорости",
            winrates,
            "first_team_first_histogram.png"
        );
    }

    @Test
    @Disabled
    public void firstTeamSecond() {
        when(randomUtils.getInIntervalNotStatic(1, 2)).thenReturn(2);
        List<Double> winrates = calcFirstTeamWinrates();
        writeHistogramPng(
            "Процент побед первой команды, когда она ходит второй",
            winrates,
            "first_team_second_histogram.png"
        );
    }

    private List<Double> calcFirstTeamWinrates() {
        final var firstTeamWinrates = new ArrayList<Double>();
        for (int i = 0; i < EXPERIMENTS; i++) {
            final var team1 = List.of(
                new BattlePersonage(
                    TestRandom.nextLong(),
                    100,
                    10,
                    44
                )
            );
            final var team2 = List.of(
                new BattlePersonage(
                    TestRandom.nextLong(),
                    100,
                    25,
                    100
                )
            );
            int firstTeamWins = 0;
            for (int j = 0; j < EXPERIMENT_ITERATIONS; j++) {
                final var firstTeam = team1.stream().map(BattlePersonage::clone).toList();
                final var secondTeam = team2.stream().map(BattlePersonage::clone).toList();
                final var result = battle.battle(firstTeam, secondTeam);
                if (result.winner() == TwoTeamBattleWinner.FIRST_TEAM) {
                    firstTeamWins++;
                }
            }
            firstTeamWinrates.add((double) firstTeamWins / EXPERIMENT_ITERATIONS * 100);
        }
        return firstTeamWinrates;
    }

    private void writeHistogramPng(
        String name,
        List<Double> winrates,
        String filename
    ) {
        double[] values = winrates.stream().mapToDouble(Double::doubleValue).toArray();
        int bins = 20; // e.g., 5% per bin

        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("Winrates", values, bins, 0.0, 100.0);

        JFreeChart histogram = ChartFactory.createHistogram(
            name,
            "Winrate (%)", // X-axis label
            "Frequency",
            dataset
        );

        try {
            ChartUtils.saveChartAsPNG(new java.io.File(filename), histogram, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<BattlePersonage> generateEqualTeam(int size) {
        final var list = new ArrayList<BattlePersonage>();
        for (int i = 0; i < size; i++) {
            list.add(
                new BattlePersonage(
                    TestRandom.nextLong(),
                    100,
                    10,
                    5
                )
            );
        }
        return list;
    }

    private static final int EXPERIMENTS = 1000;
    private static final int EXPERIMENT_ITERATIONS = 500;
}
