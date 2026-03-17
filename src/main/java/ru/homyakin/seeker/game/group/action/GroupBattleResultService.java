package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.GroupWorldRaidBattleResult;
import ru.homyakin.seeker.game.group.entity.GroupBattleResultStorage;
import ru.homyakin.seeker.game.group.entity.SavedGroupBattleResult;

import java.util.List;
import java.util.Optional;

@Component
public class GroupBattleResultService {
    private final GroupBattleResultStorage groupBattleResultStorage;

    public GroupBattleResultService(GroupBattleResultStorage groupBattleResultStorage) {
        this.groupBattleResultStorage = groupBattleResultStorage;
    }

    public void saveWorldRaidResults(List<GroupWorldRaidBattleResult> results, LaunchedEvent launchedEvent) {
        groupBattleResultStorage.saveBatch(
            results.stream()
                .map(result -> new SavedGroupBattleResult(
                    result.group().id(),
                    launchedEvent.id(),
                    result.stats()
                ))
                .toList()
        );
    }

    public Optional<SavedGroupBattleResult> getBattleResult(GroupId groupId, long launchedEventId) {
        return groupBattleResultStorage.getBattleResult(groupId, launchedEventId);
    }
}
