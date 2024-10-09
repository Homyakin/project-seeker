package ru.homyakin.seeker.game.event.raid.processing;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.utils.MathUtils;

@Component
public class RaidRewardGenerator {
    /**
     * Считает награду за рейд.
     * В случае поражения - награда равна базовой, в случае победы - награда зависит от нанесённого и полученного урона.
     * Бонус за урон считается по формуле log(1.1, урон / 10) - 43. При 1000 бонус примерно равен 5, при 3000 - 16
     */
    public int calculateReward(boolean doesParticipantsWin, PersonageBattleResult result, boolean isExhausted) {
        if (isExhausted) {
            return 0;
        }
        final int reward;
        if (!doesParticipantsWin) {
            reward = BASE_REWARD;
        } else {
            var bonusMoney = MathUtils.log(1.1, (double) result.stats().damageDealtAndTaken() / 10) - 43;
            if (bonusMoney < 0) {
                bonusMoney = 0;
            }
            reward = (int) Math.round(BASE_REWARD + bonusMoney);
        }
        return reward;
    }

    private static final int BASE_REWARD = 5;
}
