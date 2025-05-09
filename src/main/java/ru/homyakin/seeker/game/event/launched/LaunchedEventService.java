package ru.homyakin.seeker.game.event.launched;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.config.EventConfig;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestResult;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class LaunchedEventService {
    private final LaunchedEventDao launchedEventDao;
    private final LaunchedEventGroupDao launchedEventGroupDao;
    private final EventConfig config;

    public LaunchedEventService(
        LaunchedEventDao launchedEventDao,
        LaunchedEventGroupDao launchedEventGroupDao,
        EventConfig config
    ) {
        this.launchedEventDao = launchedEventDao;
        this.launchedEventGroupDao = launchedEventGroupDao;
        this.config = config;
    }

    public LaunchedEvent createLaunchedEventFromRaid(Raid raid, LocalDateTime start, GroupId groupId) {
        final var id = launchedEventDao.save(raid.eventId(), start, start.plus(config.raidDuration()));
        launchedEventGroupDao.save(id, groupId);
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

    public LaunchedEvent createFromWorldRaid(
        ActiveWorldRaid raid,
        LocalDateTime start,
        LocalDateTime end
    ) {
        final var id = launchedEventDao.save(raid.eventId(), start, end);
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

    public void updateResult(LaunchedEvent launchedEvent, PersonalQuestResult result) {
        launchedEventDao.updateStatus(
            launchedEvent.id(),
            mapResultToStatus(result)
        );
    }

    public long createFinished(PersonalQuest quest, LocalDateTime end, PersonalQuestResult result) {
        return launchedEventDao.save(quest.eventId(), end, end, mapResultToStatus(result));
    }

    public void setError(LaunchedEvent launchedEvent) {
        launchedEventDao.updateStatus(launchedEvent.id(), EventStatus.CREATION_ERROR);
    }

    public void updateResult(LaunchedEvent launchedEvent, EventResult.WorldRaidBattleResult worldRaidBattleResult) {
        launchedEventDao.updateStatus(
            launchedEvent.id(),
            worldRaidBattleResult.isWin() ? EventStatus.SUCCESS : EventStatus.FAILED
        );
    }

    public void creationError(LaunchedEvent launchedEvent) {
        launchedEventDao.updateStatus(launchedEvent.id(), EventStatus.CREATION_ERROR);
    }

    public CurrentEvents getActiveEventsByPersonageId(PersonageId personageId) {
        return new CurrentEvents(launchedEventDao.getActiveEventsByPersonageId(personageId));
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public int countFailedPersonalQuestsRowForPersonage(PersonageId personageId) {
        return launchedEventDao.countFailedPersonalQuestsRowForPersonage(personageId);
    }

    private EventStatus mapResultToStatus(PersonalQuestResult result) {
        return switch (result) {
            case PersonalQuestResult.Success _ -> EventStatus.SUCCESS;
            case PersonalQuestResult.Failure _ -> EventStatus.FAILED;
        };
    }
}
