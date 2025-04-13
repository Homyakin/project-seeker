package ru.homyakin.seeker.game.event.world_raid.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.JoinWorldRaidError;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.AddPersonageToEventRequest;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

@Component
public class JoinWorldRaidCommand {
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final PersonageService personageService;
    private final PersonageEventService personageEventService;
    private final LaunchedEventService launchedEventService;
    private final WorldRaidConfig config;

    public JoinWorldRaidCommand(
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        PersonageService personageService,
        PersonageEventService personageEventService,
        LaunchedEventService launchedEventService,
        WorldRaidConfig config
    ) {
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.personageService = personageService;
        this.personageEventService = personageEventService;
        this.launchedEventService = launchedEventService;
        this.config = config;
    }

    @Transactional
    public Either<JoinWorldRaidError, Success> execute(PersonageId personageId) {
        final var raid = getOrLaunchWorldRaidCommand.execute();
        if (!(raid.state() instanceof ActiveWorldRaidState.Battle(long launchedEventId))) {
            return Either.left(JoinWorldRaidError.NotFound.INSTANCE);
        }
        final var personage = personageService.getByIdForce(personageId);
        if (personage.tag().isEmpty()) {
            return Either.left(JoinWorldRaidError.NotInRegisteredGroup.INSTANCE);
        }
        final var personageEvents = launchedEventService.getActiveEventsByPersonageId(personageId);
        if (personageEvents.hasId(launchedEventId)) {
            return Either.left(JoinWorldRaidError.AlreadyJoined.INSTANCE);
        }
        final var energyResult = personageService.checkPersonageEnergy(personageId, config.requiredEnergy());
        if (energyResult.isLeft()) {
            return Either.left(JoinWorldRaidError.NotEnoughEnergy.INSTANCE);
        }
        final var joinResult = personageEventService.addPersonageToLaunchedEvent(
            new AddPersonageToEventRequest(launchedEventId, personageId, Optional.empty())
        );
        if (joinResult.isLeft()) {
            return Either.left(JoinWorldRaidError.NotFound.INSTANCE);
        }
        if (personageService.reduceEnergy(personage, config.requiredEnergy(), TimeUtils.moscowTime()).isLeft()) {
            throw new IllegalStateException("Failed to reduce energy after check");
        }
        return Either.right(Success.INSTANCE);
    }
}
