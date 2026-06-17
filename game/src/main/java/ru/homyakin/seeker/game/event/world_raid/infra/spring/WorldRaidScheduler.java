package ru.homyakin.seeker.game.event.world_raid.infra.spring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.action.CheckGroupsWorldRaidBattleNotificationsCommand;
import ru.homyakin.seeker.game.event.world_raid.action.CompleteWorldRaidResearchCommand;
import ru.homyakin.seeker.game.event.world_raid.action.UpdateWorldRaidBattleInfoCommand;

@Component
public class WorldRaidScheduler {
    private final CompleteWorldRaidResearchCommand completeWorldRaidResearchCommand;
    private final UpdateWorldRaidBattleInfoCommand updateWorldRaidBattleInfoCommand;
    private final CheckGroupsWorldRaidBattleNotificationsCommand checkGroupsWorldRaidBattleNotificationsCommand;

    public WorldRaidScheduler(
        CompleteWorldRaidResearchCommand completeWorldRaidResearchCommand,
        UpdateWorldRaidBattleInfoCommand updateWorldRaidBattleInfoCommand,
        CheckGroupsWorldRaidBattleNotificationsCommand checkGroupsWorldRaidBattleNotificationsCommand
    ) {
        this.completeWorldRaidResearchCommand = completeWorldRaidResearchCommand;
        this.updateWorldRaidBattleInfoCommand = updateWorldRaidBattleInfoCommand;
        this.checkGroupsWorldRaidBattleNotificationsCommand = checkGroupsWorldRaidBattleNotificationsCommand;
    }

    @Scheduled(initialDelay = 10 * 1000, fixedRate = 60 * 1000)
    public void completeResearch() {
        completeWorldRaidResearchCommand.execute();
    }

    @Scheduled(initialDelay = 10 * 1000, fixedRate = 60 * 1000)
    public void updateBattleInfo() {
        updateWorldRaidBattleInfoCommand.execute();
    }

    @Scheduled(initialDelay = 10 * 1000, fixedRate = 5 * 60 * 1000)
    public void notifyGroupsAboutBattle() {
        checkGroupsWorldRaidBattleNotificationsCommand.execute();
    }
}
