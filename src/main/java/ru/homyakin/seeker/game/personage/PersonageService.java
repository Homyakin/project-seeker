package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.personage.model.Personage;
import ru.homyakin.seeker.infrastructure.models.Success;
import ru.homyakin.seeker.game.personage.model.error.PersonageEventError;
import ru.homyakin.seeker.game.personage.model.error.EventNotExist;
import ru.homyakin.seeker.game.personage.model.error.ExpiredEvent;
import ru.homyakin.seeker.game.personage.model.error.PersonageInOtherEvent;
import ru.homyakin.seeker.game.personage.model.error.PersonageInThisEvent;

@Service
public class PersonageService {
    private static final Logger logger = LoggerFactory.getLogger(PersonageService.class);
    private final PersonageDao personageDao;
    private final LaunchedEventService launchedEventService;
    private final EventService eventService;

    public PersonageService(PersonageDao personageDao, LaunchedEventService launchedEventService, EventService eventService) {
        this.personageDao = personageDao;
        this.launchedEventService = launchedEventService;
        this.eventService = eventService;
    }

    public Personage createPersonage() {
        final var id = personageDao.save(1, 0);
        return personageDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
    }

    public Either<PersonageEventError, Success> addEvent(long personageId, Long launchedEventId) {
        final var requestedEvent = launchedEventService.getById(launchedEventId);
        if (requestedEvent.isEmpty()) {
            logger.error("Requested event " + launchedEventId + " doesn't present");
            return Either.left(new EventNotExist());
        } else if (!requestedEvent.get().isActive()) {
            return Either.left(eventService.getEventById(requestedEvent.get().eventId())
                .<PersonageEventError>map(ExpiredEvent::new)
                .orElseGet(EventNotExist::new)
            );
        }

        final var activeEvent = launchedEventService.getActiveEventByPersonageId(personageId);
        if (activeEvent.isEmpty()) {
            launchedEventService.addPersonageToLaunchedEvent(personageId, launchedEventId);
            return Either.right(new Success());
        }

        if (activeEvent.get().id() == launchedEventId) {
            return Either.left(new PersonageInThisEvent());
        } else {
            return Either.left(new PersonageInOtherEvent());
        }
    }

    public Personage addExperience(Personage personage, long exp) {
        return personage.addExperience(exp, personageDao);
    }
}
