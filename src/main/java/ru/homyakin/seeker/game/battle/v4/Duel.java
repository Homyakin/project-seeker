package ru.homyakin.seeker.game.battle.v4;

public class Duel {
    public DuelResult process(BattlePersonage first, BattlePersonage second) {
        int rounds = 0;
        while (first.isAlive() && second.isAlive()) {
            ++rounds;
            second.takeDamage(first.getDamage());
            if (second.isAlive()) {
                first.takeDamage(second.getDamage());
            }
        }
        return new DuelResult(rounds);
    }
}
