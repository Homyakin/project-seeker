package ru.homyakin.seeker.game.battle.effect;

import java.util.ArrayList;
import java.util.List;

import ru.homyakin.seeker.game.battle.BattleActionLog;
import ru.homyakin.seeker.game.battle.BattlePersonage;

/**
 * Holds transient combat modifiers for one {@link BattlePersonage}. Range bonus is aggregated for O(1) reads.
 */
public final class PersonageBattleEffects {
    private int maxRangeBonus;
    private final List<TemporaryMaxRangeBonus> rangeBonuses = new ArrayList<>();
    private final List<PeriodicDamageEffect> periodicDamages = new ArrayList<>();

    public int maxRangeBonus() {
        return maxRangeBonus;
    }

    public void addTemporaryMaxRange(TemporaryMaxRangeBonus bonus) {
        maxRangeBonus += bonus.delta();
        rangeBonuses.add(bonus);
    }

    public void addPeriodicDamage(PeriodicDamageEffect effect) {
        periodicDamages.add(effect);
    }

    public void onOwnTurnBegin(BattlePersonage self, BattleActionLog log, int round) {
        rangeBonuses.removeIf(bonus -> {
            if (bonus.tickAtOwnTurnBegin()) {
                maxRangeBonus -= bonus.delta();
                return true;
            }
            return false;
        });
        periodicDamages.removeIf(effect -> effect.tickOnOwnTurnBegin(self, log, round));
    }
}
