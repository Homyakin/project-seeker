package ru.homyakin.seeker.game.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.PersonageEventDao;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.raid.RaidResult;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.database.LaunchedEventDao;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class LaunchedEventService {
    private final LaunchedEventDao launchedEventDao;
    private final PersonageEventDao personageEventDao;
    private final GroupEventService groupEventService;
    private final LockService lockService;

    public LaunchedEventService(
        LaunchedEventDao launchedEventDao,
        PersonageEventDao personageEventDao,
        GroupEventService groupEventService,
        LockService lockService
    ) {
        this.launchedEventDao = launchedEventDao;
        this.personageEventDao = personageEventDao;
        this.groupEventService = groupEventService;
        this.lockService = lockService;
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

    public void updateResult(LaunchedEvent launchedEvent, RaidResult raidResult) {
        if (raidResult.isSuccess()) {
            launchedEventDao.updateStatus(launchedEvent.id(), EventStatus.SUCCESS);
        } else {
            launchedEventDao.updateStatus(launchedEvent.id(), EventStatus.FAILED);
        }

    }

    public void expireEvent(LaunchedEvent launchedEvent) {
        launchedEventDao.updateStatus(launchedEvent.id(), EventStatus.EXPIRED);
    }

    public void creationError(LaunchedEvent launchedEvent) {
        launchedEventDao.updateStatus(launchedEvent.id(), EventStatus.CREATION_ERROR);
    }

    public Optional<LaunchedEvent> getActiveEventByPersonageId(PersonageId personageId) {
        return launchedEventDao.getActiveByPersonageId(personageId);
    }

    public boolean addPersonageToLaunchedEvent(PersonageId personageId, long launchedEventId) {
        return lockService.tryLockAndExecute(
            LockPrefixes.LAUNCHED_EVENT.name() + launchedEventId,
            () -> personageEventDao.save(personageId, launchedEventId)
        );
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public List<GroupLaunchedEvent> getGroupEvents(LaunchedEvent launchedEvent) {
        return groupEventService.getByLaunchedEventId(launchedEvent.id());
    }
}
