package ru.homyakin.seeker.game.event.raid;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.event.raid.models.JoinToRaidResult;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.event.raid.models.AddPersonageToRaidError;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingRaid;
import ru.homyakin.seeker.utils.TimeUtils;

import java.util.Objects;
import java.util.Optional;

@Service
public class RaidService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RaidDao raidDao;
    private final PersonageService personageService;
    private final LaunchedEventService launchedEventService;
    private final RaidConfig config;

    public RaidService(
        RaidDao raidDao,
        PersonageService personageService,
        LaunchedEventService launchedEventService,
        RaidConfig config
    ) {
        this.raidDao = raidDao;
        this.personageService = personageService;
        this.launchedEventService = launchedEventService;
        this.config = config;
    }

    public Optional<Raid> getRandomRaid() {
        return raidDao.getRandomRaid();
    }

    public Optional<Raid> getByEventId(int eventId) {
        return raidDao.getByEventId(eventId);
    }

    public void save(int eventId, SavingRaid raid) {
        raidDao.save(eventId, raid);
    }

    @Transactional
    public Either<AddPersonageToRaidError, JoinToRaidResult> addPersonage(PersonageId personageId, long launchedEventId) {
        final var launchedEventResult = launchedEventService.getById(launchedEventId);
        if (launchedEventResult.isEmpty()) {
            logger.warn("Personage {} tried to join to not created event {}", personageId, launchedEventId);
            return Either.left(AddPersonageToRaidError.RaidNotExist.INSTANCE);
        }
        final var launchedEvent = launchedEventResult.get();
        final var raid = getByEventId(launchedEvent.eventId());
        if (raid.isEmpty()) {
            logger.warn("Personage {} tried to join to not raid event {}", personageId, launchedEventId);
            return Either.left(AddPersonageToRaidError.RaidNotExist.INSTANCE);
        }
        if (launchedEvent.isInFinalStatus()) {
            logger.warn("Personage {} tried to join to ended event {} in status {}", personageId, launchedEventId, launchedEvent.status());
            final var error = switch (launchedEvent.status()) {
                case LAUNCHED -> throw new IllegalStateException("Ended event can't be in launched status");
                case EXPIRED -> AddPersonageToRaidError.RaidInFinalStatus.ExpiredRaid.INSTANCE;
                case FAILED, SUCCESS -> new AddPersonageToRaidError.RaidInFinalStatus.CompletedRaid(
                    raid.get(),
                    personageService.getByLaunchedEvent(launchedEventId)
                );
                case CREATION_ERROR -> AddPersonageToRaidError.RaidInFinalStatus.CreationErrorRaid.INSTANCE;
            };
            return Either.left(error);
        }

        final var presentEvent = launchedEventService.getActiveEventByPersonageId(personageId);
        if (presentEvent.isPresent()) {
            if (Objects.equals(launchedEvent.id(), presentEvent.get().id())) {
                return Either.left(AddPersonageToRaidError.PersonageInThisRaid.INSTANCE);
            }
            return Either.left(AddPersonageToRaidError.PersonageInOtherEvent.INSTANCE);
        }

        final var checkEnergyResult = personageService.checkPersonageEnergy(
            personageId,
            config.energyCost()
        );

        if (checkEnergyResult.isLeft()) {
            return Either.left(new AddPersonageToRaidError.NotEnoughEnergy(config.energyCost()));
        }

        return launchedEventService.addPersonageToLaunchedEvent(personageId, launchedEventId)
            .<AddPersonageToRaidError>mapLeft(_ -> AddPersonageToRaidError.RaidInProcess.INSTANCE)
            .map(_ -> {
                final var reduceResult = personageService.reduceEnergy(
                    checkEnergyResult.get(),
                    config.energyCost(),
                    TimeUtils.moscowTime()
                );
                if (reduceResult.isLeft()) {
                    logger.error("Personage {} has not enough energy for raid after checking", personageId);
                    throw new IllegalStateException("Personage has not enough energy for raid after checking");
                }
                return new JoinToRaidResult(launchedEvent, raid.get(), personageService.getByLaunchedEvent(launchedEventId));
            });
    }
}
