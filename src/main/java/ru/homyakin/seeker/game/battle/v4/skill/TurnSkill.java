package ru.homyakin.seeker.game.battle.v4.skill;

import java.util.List;

import ru.homyakin.seeker.game.battle.v4.BattleContext;
import ru.homyakin.seeker.game.battle.v4.BattleEvent;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;

public sealed interface TurnSkill extends ActiveSkill {

    List<BattleEvent> apply(BattleContext context, BattlePersonage self, int round);

    non-sealed interface TurnStartSkill extends TurnSkill {
    }

    non-sealed interface TurnEndSkill extends TurnSkill {
    }
}
