package ru.homyakin.seeker.game.battle.skill;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;

public sealed interface TurnSkill extends ActiveSkill {

    List<BattleEvent> apply(BattleContext context, BattlePersonage self, int round);

    non-sealed interface TurnStartSkill extends TurnSkill {
    }

    non-sealed interface TurnEndSkill extends TurnSkill {
    }
}
