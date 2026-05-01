package ru.homyakin.seeker.game.battle.v4;

public class Duel {
    public DuelResult process(BattlePersonage first, BattlePersonage second) {
        int rounds = 0;
        while (first.isAlive() && second.isAlive()) {
            ++rounds;
            second.receiveDamageFrom(first, first.rollDamage());
            if (second.isAlive()) {
                first.receiveDamageFrom(second, second.rollDamage());
            }
        }
        return new DuelResult(rounds);
    }
}
