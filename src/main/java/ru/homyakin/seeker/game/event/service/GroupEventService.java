package ru.homyakin.seeker.game.event.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.GroupTgLaunchedEventDao;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Service
public class GroupEventService {
    private final GroupTgLaunchedEventDao groupTgLaunchedEventDao;
    private final LaunchedEventService launchedEventService;

    public GroupEventService(GroupTgLaunchedEventDao groupTgLaunchedEventDao, LaunchedEventService launchedEventService) {
        this.groupTgLaunchedEventDao = groupTgLaunchedEventDao;
        this.launchedEventService = launchedEventService;
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

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventService.getExpiredActiveEvents();
    }

    public void creationError(LaunchedEvent launchedEvent) {
        launchedEventService.creationError(launchedEvent);
    }

    public Optional<GroupLaunchedEvent> getLastEndedEventInGroup(GroupId groupId) {
        return groupTgLaunchedEventDao.lastEndedRaidInGroup(groupId);
    }

    public List<GroupLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        return groupTgLaunchedEventDao.getByLaunchedEventId(launchedEventId);
    }

    public int countFailedRaidsFromLastSuccessInGroup(GroupId groupId) {
        return groupTgLaunchedEventDao.countFailedRaidsFromLastSuccessInGroup(groupId);
    }
}
