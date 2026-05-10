package ru.homyakin.seeker.game.battle.v4.skill;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;

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
