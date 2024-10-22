package ru.homyakin.seeker.game.spin.infra.direct;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.spin.entity.GroupPersonageStatsClient;
import ru.homyakin.seeker.game.stats.action.GroupPersonageStatsService;

@Component
public class GroupPersonageStatsDirectClient implements GroupPersonageStatsClient {
    private final GroupPersonageStatsService groupPersonageStatsService;

    public GroupPersonageStatsDirectClient(GroupPersonageStatsService groupPersonageStatsService) {
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    @Override
    public void addPersonageSpinWin(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsService.addPersonageSpinWin(groupId, personageId);
    }
}
