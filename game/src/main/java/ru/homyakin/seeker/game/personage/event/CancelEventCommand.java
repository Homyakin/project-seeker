package ru.homyakin.seeker.game.personage.event;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;

@Component
public class CancelEventCommand {
    private final PersonageEventDao personageEventDao;
    private final PersonageService personageService;
    private final LockService lockService;
    private final EventService eventService;
    private final LaunchedEventService launchedEventService;

    public CancelEventCommand(
        PersonageEventDao personageEventDao,
        PersonageService personageService,
        LockService lockService,
        EventService eventService,
        LaunchedEventService launchedEventService
    ) {
        this.personageEventDao = personageEventDao;
        this.personageService = personageService;
        this.lockService = lockService;
        this.eventService = eventService;
        this.launchedEventService = launchedEventService;
    }

    /**
     * @return Возвращает добавленную энергию в случае успеха
     */
    public Either<CancelError, Integer> execute(PersonageId personageId, long launchedEventId) {
        return lockService.tryLockAndCalc(
            LockPrefixes.LAUNCHED_EVENT.name() + launchedEventId,
            () -> cancelEvent(personageId, launchedEventId)
        ).fold(
            _ -> Either.left(CancelError.Locked.INSTANCE),
            either -> either
        );
    }

    private Either<CancelError, Integer> cancelEvent(PersonageId personageId, long launchedEventId) {
        final var spentEnergyOpt = personageEventDao.getSpentEnergy(personageId, launchedEventId);
        if (spentEnergyOpt.isEmpty()) {
            return Either.left(CancelError.NotFound.INSTANCE);
        }
        final int spentEnergy = spentEnergyOpt.get();

        final var launchedEvent = launchedEventService.getById(launchedEventId).orElseThrow();
        if (launchedEvent.status().isFinal()) {
            return Either.left(CancelError.AlreadyFinished.INSTANCE);
        }

        final var refund = personageService.addEnergy(personageId, Math.max(spentEnergy - 1, 0));

        final var eventType = eventService.getEventById(launchedEvent.eventId()).orElseThrow().type();
        if (eventType == EventType.PERSONAL_QUEST) {
            launchedEventService.cancel(launchedEventId);
        } else {
            personageEventDao.delete(personageId, launchedEventId);
        }

        return Either.right(refund);
    }
}
