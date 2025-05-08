package ru.homyakin.seeker.game.personage.event;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.EventLocked;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestPersonageParams;
import ru.homyakin.seeker.game.event.raid.models.RaidPersonageParams;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.models.Success;

import java.util.List;

@Service
public class PersonageEventService {
    private final PersonageEventDao personageEventDao;
    private final PersonageService personageService;
    private final LockService lockService;

    public PersonageEventService(
        PersonageEventDao personageEventDao,
        PersonageService personageService,
        LockService lockService
    ) {
        this.personageEventDao = personageEventDao;
        this.personageService = personageService;
        this.lockService = lockService;
    }

    public Either<EventLocked, Success> addPersonageToLaunchedEvent(AddPersonageToEventRequest request) {
        return lockService.tryLockAndExecute(
            LockPrefixes.LAUNCHED_EVENT.name() + request.launchedEventId(),
            () -> personageEventDao.save(request)
        ).mapLeft(_ -> EventLocked.INSTANCE);
    }

    public List<RaidParticipant> getRaidParticipants(long launchedEventId) {
        return getEventParticipants(launchedEventId).stream()
            .map(it -> new RaidParticipant(
                it.personage(),
                (RaidPersonageParams) it.params().orElseGet(() -> new RaidPersonageParams(false))
            ))
            .toList();
    }

    public List<QuestParticipant> getQuestParticipants(long launchedEventId) {
        return getEventParticipants(launchedEventId).stream()
            .map(it -> new QuestParticipant(
                it.personage(),
                (PersonalQuestPersonageParams) it.params().orElseGet(() -> new PersonalQuestPersonageParams(1))
            ))
            .toList();
    }

    public List<WorldRaidParticipant> getWorldRaidParticipants(long launchedEventId) {
        return getEventParticipants(launchedEventId).stream()
            .map(it -> new WorldRaidParticipant(it.personage()))
            .toList();
    }

    private List<EventParticipant> getEventParticipants(long launchedEventId) {
        final var personageParams = personageEventDao.getPersonageParamsByLaunchedEvent(launchedEventId);
        return personageService.getByIds(personageParams.keySet())
            .stream()
            .map(personage -> new EventParticipant(personage, personageParams.get(personage.id())))
            .toList();
    }
}
