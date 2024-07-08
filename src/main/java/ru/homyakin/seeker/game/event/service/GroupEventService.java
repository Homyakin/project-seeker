package ru.homyakin.seeker.game.event.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.GroupTgLaunchedEventDao;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Service
public class GroupEventService {
    private final GroupTgLaunchedEventDao groupTgLaunchedEventDao;

    public GroupEventService(GroupTgLaunchedEventDao groupTgLaunchedEventDao) {
        this.groupTgLaunchedEventDao = groupTgLaunchedEventDao;
    }

    public GroupLaunchedEvent createGroupEvent(LaunchedEvent launchedEvent, Group group, Integer messageId) {
        var groupEvent = new GroupLaunchedEvent(
            launchedEvent.id(),
            group.id(),
            messageId
        );
        groupTgLaunchedEventDao.save(groupEvent);
        return groupEvent;
    }

    public List<GroupLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        return groupTgLaunchedEventDao.getByLaunchedEventId(launchedEventId);
    }

    public int countFailedEventsFromLastSuccessInGroup(GroupId groupId) {
        return groupTgLaunchedEventDao.countFailedEventsFromLastSuccessInGroup(groupId);
    }
}
