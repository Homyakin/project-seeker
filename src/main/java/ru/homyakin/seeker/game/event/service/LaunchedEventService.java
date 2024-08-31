package ru.homyakin.seeker.game.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.config.EventConfig;
import ru.homyakin.seeker.game.event.database.PersonageEventDao;
import ru.homyakin.seeker.game.event.models.EventLocked;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.game.event.database.LaunchedEventDao;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

@Service
public class LaunchedEventService {
    private final LaunchedEventDao launchedEventDao;
    private final PersonageEventDao personageEventDao;
    private final GroupEventService groupEventService;
    private final LockService lockService;
    private final EventConfig config;

    public LaunchedEventService(
        LaunchedEventDao launchedEventDao,
        PersonageEventDao personageEventDao,
        GroupEventService groupEventService,
        LockService lockService,
        EventConfig config
    ) {
        this.launchedEventDao = launchedEventDao;
        this.personageEventDao = personageEventDao;
        this.groupEventService = groupEventService;
        this.lockService = lockService;
        this.config = config;
    }

    public LaunchedEvent createLaunchedEventFromRaid(Raid raid, LocalDateTime start) {
        final var id = launchedEventDao.save(raid.eventId(), start, start.plus(config.raidDuration()));
        return getById(id).orElseThrow(() -> new IllegalStateException("Launched event must be present after create"));
    }

    public LaunchedEvent createFromPersonalQuest(
        PersonalQuest quest,
        LocalDateTime start,
        LocalDateTime end
    ) {
        final var id = launchedEventDao.save(quest.eventId(), start, end);
        return getById(id).orElseThrow(() -> new IllegalStateException("Launched event must be present after create"));
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        return launchedEventDao.getById(launchedEventId);
    }

    public GroupLaunchedEvent addGroupMessage(LaunchedEvent launchedEvent, Group group, Integer messageId) {
        return groupEventService.createGroupEvent(launchedEvent, group, messageId);
    }

    public void updateResult(LaunchedEvent launchedEvent, EventResult.Raid raidResult) {
        launchedEventDao.updateStatus(
            launchedEvent.id(),
            switch (raidResult.status()) {
                case SUCCESS -> EventStatus.SUCCESS;
                case FAILURE -> EventStatus.FAILED;
                case EXPIRED -> EventStatus.EXPIRED;
            }
        );
    }

    public void updateResult(LaunchedEvent launchedEvent, EventResult.PersonalQuestResult personalQuestResult) {
        launchedEventDao.updateStatus(
            launchedEvent.id(),
            switch (personalQuestResult) {
                case EventResult.PersonalQuestResult.Error _ -> EventStatus.CREATION_ERROR;
                case EventResult.PersonalQuestResult.Failure _ -> EventStatus.FAILED;
                case EventResult.PersonalQuestResult.Success _ -> EventStatus.SUCCESS;
            }
        );
    }

    public void creationError(LaunchedEvent launchedEvent) {
        launchedEventDao.updateStatus(launchedEvent.id(), EventStatus.CREATION_ERROR);
    }

    public Optional<LaunchedEvent> getActiveEventByPersonageId(PersonageId personageId) {
        return launchedEventDao.getActiveByPersonageId(personageId);
    }

    public Either<EventLocked, Success> addPersonageToLaunchedEvent(PersonageId personageId, long launchedEventId) {
        return lockService.tryLockAndExecute(
            LockPrefixes.LAUNCHED_EVENT.name() + launchedEventId,
            () -> personageEventDao.save(personageId, launchedEventId)
        ).mapLeft(_ -> EventLocked.INSTANCE);
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public List<GroupLaunchedEvent> getGroupEvents(LaunchedEvent launchedEvent) {
        return groupEventService.getByLaunchedEventId(launchedEvent.id());
    }

    public Optional<LaunchedEvent> getLastEndedEventInGroup(GroupId groupId) {
        return launchedEventDao.lastEndedEventInGroup(groupId);
    }
}
