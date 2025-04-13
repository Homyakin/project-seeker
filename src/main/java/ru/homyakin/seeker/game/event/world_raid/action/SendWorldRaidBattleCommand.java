package ru.homyakin.seeker.game.event.world_raid.action;

import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;

public interface SendWorldRaidBattleCommand {
    void sendBattle(ActiveWorldRaid raid, LaunchedEvent event, int requiredEnergy);
}
