package ru.homyakin.seeker.game.event.world_raid.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UpdateWorldRaidBattleInfoCommand {
    private final WorldRaidConfig config;
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final LaunchedEventService launchedEventService;
    private final PersonageEventService personageEventService;
    private final SendWorldRaidBattleUpdateCommand sendWorldRaidBattleUpdateCommand;
    private final GetGroup getGroup;

    public UpdateWorldRaidBattleInfoCommand(
        WorldRaidConfig config,
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        LaunchedEventService launchedEventService,
        PersonageEventService personageEventService,
        SendWorldRaidBattleUpdateCommand sendWorldRaidBattleUpdateCommand,
        GetGroup getGroup
    ) {
        this.config = config;
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.launchedEventService = launchedEventService;
        this.personageEventService = personageEventService;
        this.sendWorldRaidBattleUpdateCommand = sendWorldRaidBattleUpdateCommand;
        this.getGroup = getGroup;
    }

    public void execute() {
        final var raid = getOrLaunchWorldRaidCommand.execute();
        if (!(raid.state() instanceof ActiveWorldRaidState.Battle(long launchedEventId))) {
            return;
        }
        final var launchedEvent = launchedEventService.getById(launchedEventId).orElseThrow();
        final var participants = personageEventService.getWorldRaidParticipants(launchedEvent.id());
        final var groupTags = participants.stream()
            .map(personageEvent -> personageEvent.personage().tag())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
        sendWorldRaidBattleUpdateCommand.sendUpdate(
            raid,
            launchedEvent,
            participants.size(),
            getGroup.getByTags(groupTags.stream().toList()),
            config.requiredEnergy()
        );
    }
}
