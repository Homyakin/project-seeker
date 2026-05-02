package ru.homyakin.seeker.game.battle.v4;

import org.junit.jupiter.api.Test;

import java.util.List;

public class BattleSimulator {
    @Test
    public void simple() {
        int iterations = 100_000;
        int firstWins = 0;
        int totalRounds = 0;
        int maxRounds = 0;
        long sumFirstDamageTaken = 0;
        long sumFirstDefenceEvents = 0;
        long sumSecondDamageTaken = 0;
        long sumSecondDefenceEvents = 0;
        int winsFirstAlive = 0;
        int winsSecondAlive = 0;
        int winsBothAlive = 0;

        for (int i = 0; i < iterations; i++) {
            // Team1: Танк на FRONT прикрывает Лучника на MID
            var firstTeam = teamTankArcher();

            // Team2: Два ассасина на FRONT (range=1 — не достают до лучника)
            var secondTeam = teamTwoArchers();

            //System.out.println(firstTeam.getFirst().power());
            //System.out.println(secondTeam.getFirst().power());

            var result = new Battle().process(firstTeam, secondTeam);

            if (result.firstWin()) {
                firstWins++;
                final boolean firstAlive = firstTeam.getFirst().isAlive();
                final boolean secondAlive = firstTeam.get(1).isAlive();
                if (firstAlive && !secondAlive) {
                    winsFirstAlive++;
                }
                if (secondAlive && !firstAlive) {
                    winsSecondAlive++;
                }
                if (firstAlive && secondAlive) {
                    winsBothAlive++;
                }
            }
            totalRounds += result.rounds();
            maxRounds = Math.max(maxRounds, result.rounds());

            final var s0 = firstTeam.getFirst().battlePersonageStats();
            final var s1 = firstTeam.get(1).battlePersonageStats();
            sumFirstDamageTaken += s0.damageBlocked() + s0.damageDodged();
            sumFirstDefenceEvents += s0.blockCount() + s0.dodgesCount();
            sumSecondDamageTaken += s1.damageBlocked() + s1.damageDodged();
            sumSecondDefenceEvents += s1.blockCount() + s1.dodgesCount();
        }

        System.out.println("First wins:   " + firstWins + " / " + iterations +
            " (" + (firstWins * 100.0 / iterations) + "%)");
        if (firstWins > 0) {
            System.out.printf(
                "When first wins — 1st alive: %.2f%%, 2nd alive: %.2f%%, both alive: %.2f%%%n",
                winsFirstAlive * 100.0 / firstWins,
                winsSecondAlive * 100.0 / firstWins,
                winsBothAlive * 100.0 / firstWins
            );
        } else {
            System.out.println("When first wins — (no first-team wins in sample)");
        }
        System.out.println("Avg rounds:   " + totalRounds / iterations);
        System.out.println("Max rounds:   " + maxRounds);
        System.out.printf(
            "Team1 1st (tank):     avg incoming dmg (block+dodge rolls)=%.2f, avg block+dodge events=%.2f%n",
            sumFirstDamageTaken / (double) iterations,
            sumFirstDefenceEvents / (double) iterations
        );
        System.out.printf(
            "Team1 2nd (assassin): avg incoming dmg (block+dodge rolls)=%.2f, avg block+dodge events=%.2f%n",
            sumSecondDamageTaken / (double) iterations,
            sumSecondDefenceEvents / (double) iterations
        );
    }

    public List<BattlePersonage> teamTankAssassin() {
        return List.of(
            // 🛡️ Танк — высокий baseThreat, много HP, средний урон
            new BattlePersonage(2500, 150, 320, 5, 5, 1.5, 140, 100, Position.FRONT, 1),

            // 🗡️ Ассасин — низкий baseThreat, высокий урон, быстрый
            new BattlePersonage(600, 380, 80, 20, 25, 2.0, 220, 10, Position.FRONT, 1)
        );
    }

    public List<BattlePersonage> teamTankArcher() {
        return List.of(
            new BattlePersonage(2500, 150, 320, 5,  5,  1.5, 140, 100, Position.FRONT, 1), // Танк
            new BattlePersonage(880,  360,  60, 15, 15, 1.75, 200,  15, Position.MID,   2)  // Лучник
        );
    }
    public List<BattlePersonage> teamTwoArchers() {
        return List.of(
            new BattlePersonage(880,  360,  60, 15, 15, 1.75, 200,  15, Position.FRONT,   2),  // Лучник
            new BattlePersonage(880,  360,  60, 15, 15, 1.75, 200,  15, Position.MID,   2)  // Лучник
        );
    }

    public List<BattlePersonage> teamTwoKillers() {
        return List.of(
            new BattlePersonage(600, 380, 80, 20, 25, 2.0, 220, 10, Position.FRONT, 1),
            new BattlePersonage(600, 380, 80, 20, 25, 2.0, 220, 10, Position.FRONT, 1)
        );
    }

    public List<BattlePersonage> teamTankMage() {
        return List.of(
            new BattlePersonage(2500, 150, 320,  5,  5, 1.5,  140, 100, Position.FRONT, 1),
            new BattlePersonage(800,  350,  60, 20, 10, 1.75, 180,   5, Position.BACK,  3)
        );
    }


    @Test
    public void power() {
        System.out.println(new BattlePersonage(880,  360,  60, 15, 15, 1.75, 200,  15, Position.MID,   2).power());
        System.out.println(new BattlePersonage(2500, 150, 320, 5,  5,  1.5, 140, 100, Position.FRONT, 1).power());
    }
}
