package ru.homyakin.seeker.game.event.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.GroupLaunchedEventDao;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.telegram.group.models.Group;

@Service
public class GroupEventService {
    private final GroupLaunchedEventDao groupLaunchedEventDao;

    public GroupEventService(GroupLaunchedEventDao groupLaunchedEventDao) {
        this.groupLaunchedEventDao = groupLaunchedEventDao;
    }

    public GroupLaunchedEvent createGroupEvent(LaunchedEvent launchedEvent, Group group, Integer messageId) {
        var groupEvent = new GroupLaunchedEvent(
            launchedEvent.id(),
            group.id(),
            messageId
        );
        groupLaunchedEventDao.save(groupEvent);
        return groupEvent;
    }

    public List<GroupLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        return groupLaunchedEventDao.getByLaunchedEventId(launchedEventId);
    }
}
