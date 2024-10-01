package ru.homyakin.seeker.game.event.launched;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.config.EventConfig;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class LaunchedEventService {
    private final LaunchedEventDao launchedEventDao;
    private final EventConfig config;

    public LaunchedEventService(
        LaunchedEventDao launchedEventDao,
        EventConfig config
    ) {
        this.launchedEventDao = launchedEventDao;
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

    public void updateResult(LaunchedEvent launchedEvent, EventResult.RaidResult raidResult) {
        launchedEventDao.updateStatus(
            launchedEvent.id(),
            switch (raidResult) {
                case EventResult.RaidResult.Completed completed -> switch (completed.status()) {
                    case SUCCESS -> EventStatus.SUCCESS;
                    case FAILURE -> EventStatus.FAILED;
                };
                case EventResult.RaidResult.Expired _ -> EventStatus.EXPIRED;
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

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public int countFailedPersonalQuestsRowForPersonage(PersonageId personageId) {
        return launchedEventDao.countFailedPersonalQuestsRowForPersonage(personageId);
    }
}
