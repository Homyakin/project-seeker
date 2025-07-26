package ru.homyakin.seeker.game.event.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.GroupTgLaunchedEventDao;
import ru.homyakin.seeker.game.event.database.GroupEventServiceDao;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.group.action.UpdateGroupParameters;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

@Service
public class GroupEventService {
    private final GroupTgLaunchedEventDao groupTgLaunchedEventDao;
    private final GroupEventServiceDao groupEventServiceDao;
    private final LaunchedEventService launchedEventService;
    private final UpdateGroupParameters updateGroupParameters;

    public GroupEventService(
        GroupTgLaunchedEventDao groupTgLaunchedEventDao,
        GroupEventServiceDao groupEventServiceDao,
        LaunchedEventService launchedEventService,
        UpdateGroupParameters updateGroupParameters
    ) {
        this.groupTgLaunchedEventDao = groupTgLaunchedEventDao;
        this.groupEventServiceDao = groupEventServiceDao;
        this.launchedEventService = launchedEventService;
        this.updateGroupParameters = updateGroupParameters;
    }

    public void createGroupEvent(long launchedEventId, GroupTg group, Integer messageId) {
        var groupEvent = new GroupLaunchedEvent(
            launchedEventId,
            group.id(),
            messageId
        );
        groupTgLaunchedEventDao.save(groupEvent);
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventService.getExpiredActiveEvents();
    }

    public void creationError(long launchedEventId) {
        launchedEventService.creationError(launchedEventId);
    }

    public Optional<GroupLaunchedEvent> getLastEndedEventInGroup(GroupTgId groupId) {
        return groupTgLaunchedEventDao.lastEndedRaidInGroup(groupId);
    }

    public List<GroupLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        return groupTgLaunchedEventDao.getByLaunchedEventId(launchedEventId);
    }

    public void updateRaidLevel(long launchedEventId, boolean wasRaidSuccess) {
        final var groupId = groupEventServiceDao.getGroupsByLaunchedEventId(launchedEventId)
            .getFirst();
        updateGroupParameters.updateRaidLevel(groupId, wasRaidSuccess);
    }
}
