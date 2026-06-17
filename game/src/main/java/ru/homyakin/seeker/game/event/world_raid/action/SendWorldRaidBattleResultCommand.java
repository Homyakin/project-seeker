package ru.homyakin.seeker.game.event.world_raid.action;

import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;

public interface SendWorldRaidBattleResultCommand {
    void sendBattleResult(
        EventResult.WorldRaidBattleResult result,
        ActiveWorldRaid raid
    );
}
