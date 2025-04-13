package ru.homyakin.seeker.game.event.world_raid.action;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.battle.two_team.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.FinalWorldRaidStatus;
import ru.homyakin.seeker.game.event.world_raid.entity.ResearchGenerator;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleGenerator;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleInfo;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.WorldRaidBattleResultService;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.WorldRaidParticipant;
import ru.homyakin.seeker.game.personage.models.Personage;

import java.util.List;

@Component
public class ProcessWorldRaidBattleCommand {
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final PersonageEventService personageEventService;
    private final TwoPersonageTeamsBattle twoPersonageTeamsBattle;
    private final WorldRaidBattleGenerator battleGenerator;
    private final ResearchGenerator researchGenerator;
    private final WorldRaidStorage storage;
    private final WorldRaidBattleResultService worldRaidBattleResultService;
    private final SendWorldRaidBattleResultCommand sendWorldRaidBattleResultCommand;
    private final LaunchedEventService launchedEventService;

    public ProcessWorldRaidBattleCommand(
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        PersonageEventService personageEventService,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle,
        WorldRaidBattleGenerator battleGenerator,
        ResearchGenerator researchGenerator,
        WorldRaidStorage storage,
        WorldRaidBattleResultService worldRaidBattleResultService,
        SendWorldRaidBattleResultCommand sendWorldRaidBattleResultCommand,
        LaunchedEventService launchedEventService
    ) {
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.personageEventService = personageEventService;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
        this.battleGenerator = battleGenerator;
        this.researchGenerator = researchGenerator;
        this.storage = storage;
        this.worldRaidBattleResultService = worldRaidBattleResultService;
        this.sendWorldRaidBattleResultCommand = sendWorldRaidBattleResultCommand;
        this.launchedEventService = launchedEventService;
    }

    @Transactional
    public EventResult.WorldRaidBattleResult execute(LaunchedEvent launchedEvent) {
        final var raid = getOrLaunchWorldRaidCommand.execute();
        if (!(raid.state() instanceof ActiveWorldRaidState.Battle)) {
            throw new IllegalStateException("Invalid raid state: " + raid.state());
        }
        final var participants = personageEventService.getWorldRaidParticipants(launchedEvent.id());
        final var personageTeam = participants.stream()
            .map(WorldRaidParticipant::personage)
            .map(Personage::toBattlePersonage)
            .toList();
        final var result = twoPersonageTeamsBattle.battle(
            battleGenerator.generate(raid),
            personageTeam
        );
        final var remainedInfo = remainedBattleInfo(result.firstTeamResults().personageResults());
        final var status = switch (result.winner()) {
            case FIRST_TEAM -> {
                storage.saveAsContinued(
                    raid,
                    remainedInfo,
                    researchGenerator.generate()
                );
                yield FinalWorldRaidStatus.CONTINUED;
            }
            case SECOND_TEAM -> {
                storage.setStatus(raid.id(), FinalWorldRaidStatus.FINISHED);
                yield FinalWorldRaidStatus.FINISHED;
            }
        };
        final var worldRaidResult = worldRaidBattleResultService.processResult(
            raid.fund(),
            result.secondTeamResults(),
            status == FinalWorldRaidStatus.FINISHED,
            launchedEvent,
            remainedInfo
        );
        sendWorldRaidBattleResultCommand.sendBattleResult(worldRaidResult, raid);
        launchedEventService.updateResult(launchedEvent, worldRaidResult);
        return worldRaidResult;
    }

    private WorldRaidBattleInfo remainedBattleInfo(List<PersonageBattleResult> results) {
        final var stats = results.getFirst().stats();
        return new WorldRaidBattleInfo(
            stats.remainHealth(),
            stats.characteristics().attack(),
            stats.characteristics().defense(),
            stats.characteristics().strength(),
            stats.characteristics().agility(),
            stats.characteristics().wisdom()
        );
    }
}
