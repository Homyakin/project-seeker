package ru.homyakin.seeker.game.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.UserEventDao;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.database.LaunchedEventDao;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class LaunchedEventService {
    private final LaunchedEventDao launchedEventDao;
    private final UserEventDao userEventDao;

    private final GroupEventService groupEventService;

    public LaunchedEventService(
        LaunchedEventDao launchedEventDao,
        UserEventDao userEventDao,
        GroupEventService groupEventService
    ) {
        this.launchedEventDao = launchedEventDao;
        this.userEventDao = userEventDao;
        this.groupEventService = groupEventService;
    }

    public LaunchedEvent createLaunchedEvent(Event event) {
        final var id = launchedEventDao.save(event);
        return getById(id).orElseThrow(() -> new IllegalStateException("Launched event must be present after create"));
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        return launchedEventDao.getById(launchedEventId);
    }

    public GroupLaunchedEvent addGroupMessage(LaunchedEvent launchedEvent, Group group, Integer messageId) {
        return groupEventService.createGroupEvent(launchedEvent, group, messageId);
    }

    public void updateActive(LaunchedEvent launchedEvent, boolean isActive) {
        launchedEventDao.updateIsActive(launchedEvent.id(), isActive);
    }

    public Optional<LaunchedEvent> getActiveEventByPersonageId(Long personageId) {
        return launchedEventDao.getActiveByPersonageId(personageId);
    }

    public void addPersonageToLaunchedEvent(Long personageId, Long launchedEventId) {
        userEventDao.save(personageId, launchedEventId);
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public List<GroupLaunchedEvent> getGroupEvents(LaunchedEvent launchedEvent) {
        return groupEventService.getByLaunchedEventId(launchedEvent.id());
    }
}
