package ru.homyakin.seeker.game.event.world_raid.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.game.group.action.GroupBattleResultService;
import ru.homyakin.seeker.game.group.entity.SavedGroupBattleResult;

import java.util.Optional;

@Component
public class GroupWorldRaidBattleResultCommand {
    private final WorldRaidStorage storage;
    private final GroupBattleResultService groupBattleResultService;

    public GroupWorldRaidBattleResultCommand(
        WorldRaidStorage storage,
        GroupBattleResultService groupBattleResultService
    ) {
        this.storage = storage;
        this.groupBattleResultService = groupBattleResultService;
    }

    public Optional<SavedGroupBattleResult> getForLastWorldRaid(GroupId groupId) {
        return storage.getLaunchedEventIdForLastFinished()
            .flatMap(launchedEventId -> groupBattleResultService.getBattleResult(groupId, launchedEventId));
    }
}
