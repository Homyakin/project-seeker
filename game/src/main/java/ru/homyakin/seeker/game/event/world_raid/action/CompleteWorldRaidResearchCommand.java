package ru.homyakin.seeker.game.event.world_raid.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.PersonageContribution;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CompleteWorldRaidResearchCommand {
    private static final Logger logger = LoggerFactory.getLogger(CompleteWorldRaidResearchCommand.class);
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final NotifyAboutWorldRaidResearchEndCommand notifyAboutWorldRaidResearchEndCommand;
    private final SendWorldRaidBattleCommand sendWorldRaidBattleCommand;
    private final LaunchedEventService launchedEventService;
    private final WorldRaidStorage storage;
    private final WorldRaidConfig config;
    private final PersonageService personageService;

    public CompleteWorldRaidResearchCommand(
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        NotifyAboutWorldRaidResearchEndCommand notifyAboutWorldRaidResearchEndCommand,
        SendWorldRaidBattleCommand sendWorldRaidBattleCommand,
        LaunchedEventService launchedEventService,
        WorldRaidStorage storage,
        WorldRaidConfig config,
        PersonageService personageService
    ) {
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.notifyAboutWorldRaidResearchEndCommand = notifyAboutWorldRaidResearchEndCommand;
        this.sendWorldRaidBattleCommand = sendWorldRaidBattleCommand;
        this.launchedEventService = launchedEventService;
        this.storage = storage;
        this.config = config;
        this.personageService = personageService;
    }

    @Transactional
    public void execute() {
        final var raid = getOrLaunchWorldRaidCommand.execute();

        if (!(raid.state() instanceof ActiveWorldRaidState.Research research && research.isCompleted())) {
            return;
        }
        logger.info("Research of world raid {} is completed", raid.code());
        final var rewards = calculateRewards(
            storage.getPersonageContributions(raid.id()),
            raid.fund(),
            research.contribution()
        );
        final var totalReward = rewards.values().stream().mapToInt(Money::value).sum();
        storage.setRewards(raid.id(), rewards);
        personageService.addMoneyBatch(rewards);
        storage.updateFund(raid.id(), raid.fund().add(Money.from(totalReward).negative()));
        notifyAboutWorldRaidResearchEndCommand.notifyAboutResearchEnd();
        final var launchedEvent = launchedEvent(raid);
        storage.setBattleState(raid.id(), launchedEvent.id());
        sendWorldRaidBattleCommand.sendBattle(
            getOrLaunchWorldRaidCommand.execute(), // Нужно получить новый, так как обновился фонд
            launchedEvent,
            config.requiredEnergy()
        );
    }

    private Map<PersonageId, Money> calculateRewards(
        List<PersonageContribution> contributions,
        Money fund,
        int totalContribution
    ) {
        final float totalReward = Math.round(fund.value() * 0.15f);
        return contributions.stream()
            .map(contribution ->
                Map.entry(
                    contribution.personageId(),
                    Money.from(
                        (int) ((totalReward * contribution.contribution()) / totalContribution)
                    )
                )
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private LaunchedEvent launchedEvent(ActiveWorldRaid raid) {
        final var now = TimeUtils.moscowTime();
        return launchedEventService.createFromWorldRaid(
            raid,
            now,
            now.plus(config.battleDuration())
        );
    }
}
