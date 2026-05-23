package ru.homyakin.seeker.game.battle.skill;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;

public sealed interface DamageDealSkill extends ActiveSkill {

    List<BattleEvent> apply(BattleContext context, BattlePersonage self, BattlePersonage target, int round);

    non-sealed interface OnHitSkill extends DamageDealSkill {
    }

    non-sealed interface OnCritSkill extends DamageDealSkill {
    }

    non-sealed interface OnMissSkill extends DamageDealSkill {
    }
    
    non-sealed interface OnDamageReceiveSkill extends DamageDealSkill {
    }

    non-sealed interface OnCritReceiveSkill extends DamageDealSkill {
    }

    non-sealed interface OnDodgeSkill extends DamageDealSkill {
    }
}
