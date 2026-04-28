package ru.homyakin.seeker.game.battle.v4;

import org.junit.jupiter.api.Test;

import java.util.List;

public class BattleSimulator {
    @Test
    public void simple() {
        int iterations = 1;
        int firstWins = 0;
        int totalRounds = 0;
        int maxRounds = 0;

        for (int i = 0; i < iterations; i++) {
            var firstTeam = List.of(
                new BattlePersonage(2500, 150, 320, 5, 5, 1.5, 140)
            );
            var secondTeam = List.of(
                new BattlePersonage(600, 380, 80, 20, 25, 2.0, 220)
            );

            System.out.println(firstTeam.getFirst().power());
            System.out.println(secondTeam.getFirst().power());

            var result = new Battle().process(firstTeam, secondTeam);

            if (result.firstWin()) firstWins++;
            totalRounds += result.rounds();
            maxRounds = Math.max(maxRounds, result.rounds());
        }

        System.out.println("First wins:   " + firstWins + " / " + iterations +
            " (" + (firstWins * 100 / iterations) + "%)");
        System.out.println("Avg rounds:   " + totalRounds / iterations);
        System.out.println("Max rounds:   " + maxRounds);
    }
}
