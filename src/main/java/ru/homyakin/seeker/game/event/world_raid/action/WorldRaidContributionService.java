package ru.homyakin.seeker.game.event.world_raid.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidResearchDonateError;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class WorldRaidContributionService {
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final PersonageService personageService;
    private final WorldRaidStorage storage;
    private final WorldRaidConfig config;

    public WorldRaidContributionService(
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        PersonageService personageService,
        WorldRaidStorage storage, WorldRaidConfig config
    ) {
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.personageService = personageService;
        this.storage = storage;
        this.config = config;
    }

    /**
     * @return Возвращает сколько денег было добавлено к фонду
     */
    @Transactional
    public Either<WorldRaidResearchDonateError, Money> donate(PersonageId personageId) {
        final var active = getOrLaunchWorldRaidCommand.execute();
        if (
            active.state() instanceof ActiveWorldRaidState.Research research &&
                research.isInProgress()
        ) {
            final var takeMoneyResult = personageService.tryTakeMoney(personageId, config.requiredForDonate());
            if (takeMoneyResult.isLeft()) {
                return Either.left(
                    new WorldRaidResearchDonateError.NotEnoughMoney(takeMoneyResult.getLeft().neededMoney())
                );
            }
            storage.incrementContribution(active.id(), personageId, config.fundFromDonation());
            return Either.right(config.fundFromDonation());
        }
        return Either.left(WorldRaidResearchDonateError.ResearchCompleted.INSTANCE);
    }

    @Transactional
    public void questComplete(PersonageId personageId) {
        final var active = getOrLaunchWorldRaidCommand.execute();
        if (
            active.state() instanceof ActiveWorldRaidState.Research research &&
                research.isInProgress()
        ) {
            storage.incrementContribution(active.id(), personageId, config.fundFromQuest());
        }
    }
}
