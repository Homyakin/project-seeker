package ru.homyakin.seeker.game.event.world_raid.action;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.battle.result.GroupBattleResult;
import ru.homyakin.seeker.game.battle.result.PersonageBattleResult;
import ru.homyakin.seeker.game.battle.result.TeamResult;
import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.battle.GroupBattleStats;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.FinalWorldRaidStatus;
import ru.homyakin.seeker.game.event.world_raid.entity.ResearchGenerator;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleGenerator;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.WorldRaidBattleResultService;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.WorldRaidParticipant;
import ru.homyakin.seeker.game.stats.action.GroupStatsService;
import ru.homyakin.seeker.game.stats.action.PersonageStatsService;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProcessWorldRaidBattleCommand {
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final PersonageEventService personageEventService;
    private final ItemService itemService;
    private final Battle battle = new Battle();
    private final WorldRaidBattleGenerator battleGenerator;
    private final ResearchGenerator researchGenerator;
    private final WorldRaidStorage storage;
    private final WorldRaidBattleResultService worldRaidBattleResultService;
    private final SendWorldRaidBattleResultCommand sendWorldRaidBattleResultCommand;
    private final LaunchedEventService launchedEventService;
    private final GroupStatsService groupStatsService;
    private final PersonageStatsService personageStatsService;
    private final WorldRaidConfig config;

    public ProcessWorldRaidBattleCommand(
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        PersonageEventService personageEventService,
        ItemService itemService,
        WorldRaidBattleGenerator battleGenerator,
        ResearchGenerator researchGenerator,
        WorldRaidStorage storage,
        WorldRaidBattleResultService worldRaidBattleResultService,
        SendWorldRaidBattleResultCommand sendWorldRaidBattleResultCommand,
        LaunchedEventService launchedEventService,
        GroupStatsService groupStatsService,
        PersonageStatsService personageStatsService,
        WorldRaidConfig config
    ) {
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.personageEventService = personageEventService;
        this.itemService = itemService;
        this.battleGenerator = battleGenerator;
        this.researchGenerator = researchGenerator;
        this.storage = storage;
        this.worldRaidBattleResultService = worldRaidBattleResultService;
        this.sendWorldRaidBattleResultCommand = sendWorldRaidBattleResultCommand;
        this.launchedEventService = launchedEventService;
        this.groupStatsService = groupStatsService;
        this.personageStatsService = personageStatsService;
        this.config = config;
    }

    @Transactional
    public EventResult.WorldRaidBattleResult execute(LaunchedEvent launchedEvent) {
        final var raid = getOrLaunchWorldRaidCommand.execute();
        if (!(raid.state() instanceof ActiveWorldRaidState.Battle)) {
            throw new IllegalStateException("Invalid raid state: " + raid.state());
        }
        final var participants = personageEventService.getWorldRaidParticipants(launchedEvent.id());
        final var personageTeam = toBattlePersonages(participants);
        final var enemies = battleGenerator.generate(raid);
        final var result = battle.process(enemies, personageTeam);
        final var remainedInfo = battleGenerator.remainedInfo(raid.info(), enemies, result.personageStats());
        final var doesParticipantsWin = !result.firstWin();
        if (doesParticipantsWin) {
            storage.setStatus(raid.id(), FinalWorldRaidStatus.FINISHED);
        } else {
            storage.saveAsContinued(
                raid,
                remainedInfo,
                Money.from(RandomUtils.getInPercentRange(config.initFund().value(), 10)),
                researchGenerator.generate()
            );
        }
        final var participantResult = toTeamResult(participants, personageTeam, result.personageStats());
        final var worldRaidResult = worldRaidBattleResultService.processResult(
            raid.fund(),
            participantResult,
            doesParticipantsWin,
            launchedEvent,
            remainedInfo
        );

        sendWorldRaidBattleResultCommand.sendBattleResult(worldRaidResult, raid);
        launchedEventService.updateResult(launchedEvent, worldRaidResult);
        for (final var groupResult : worldRaidResult.groupResults()) {
            if (worldRaidResult.isWin()) {
                groupStatsService.addSuccessWoldRaid(groupResult.group().id());
            } else {
                groupStatsService.addFailedWoldRaid(groupResult.group().id());
            }
        }
        for (final var personageResult : worldRaidResult.personageResults()) {
            if (worldRaidResult.isWin()) {
                personageStatsService.addSuccessWorldRaid(personageResult.personage().id());
            } else {
                personageStatsService.addFailedWorldRaid(personageResult.personage().id());
            }
        }
        return worldRaidResult;
    }

    private List<BattlePersonage> toBattlePersonages(List<WorldRaidParticipant> participants) {
        final var personageIds = participants.stream()
            .map(participant -> participant.personage().id())
            .collect(Collectors.toSet());
        final var equippedItemsByPersonageId = itemService.getEquippedItemsByPersonageIds(personageIds);
        return participants.stream()
            .map(participant -> BattlePersonage.forCombat(
                equippedItemsByPersonageId.getOrDefault(participant.personage().id(), List.of()),
                participant.personage().position(),
                participant.personage().effects()
            ))
            .toList();
    }

    private TeamResult toTeamResult(
        List<WorldRaidParticipant> participants,
        List<BattlePersonage> personageTeam,
        java.util.Map<UUID, BattlePersonageStats> personageStats
    ) {
        final var groupAccumulators = new HashMap<String, GroupStatsAccumulator>();
        final var personageResults = new java.util.ArrayList<PersonageBattleResult>(participants.size());
        for (int i = 0; i < participants.size(); i++) {
            final var participant = participants.get(i);
            final var stats = personageStats.get(personageTeam.get(i).id());
            personageResults.add(new PersonageBattleResult(participant.personage(), stats));
            participant.personage().tag().ifPresent(tag -> groupAccumulators
                .computeIfAbsent(tag, _ -> new GroupStatsAccumulator())
                .add(stats)
            );
        }
        final var groupResults = groupAccumulators.entrySet().stream()
            .map(it -> new GroupBattleResult(it.getKey(), it.getValue().toStats()))
            .toList();
        return new TeamResult(groupResults, personageResults);
    }

    private static class GroupStatsAccumulator {
        private long remainHealth = 0L;
        private long totalHealth = 0L;
        private long normalDamageDealt = 0L;
        private long normalAttackCount = 0L;
        private long critDamageDealt = 0L;
        private long critsCount = 0L;
        private long damageBlocked = 0L;
        private long blockCount = 0L;
        private long damageDodged = 0L;
        private long dodgesCount = 0L;
        private long missesCount = 0L;
        private int totalPersonages = 0;
        private int remainPersonages = 0;

        private void add(BattlePersonageStats stats) {
            remainHealth += stats.remainHealth();
            totalHealth += stats.initialHealth();
            normalDamageDealt += stats.normalDamageDealt();
            normalAttackCount += stats.normalAttackCount();
            critDamageDealt += stats.critDamageDealt();
            critsCount += stats.critsCount();
            damageBlocked += stats.damageBlocked();
            blockCount += stats.blockCount();
            damageDodged += stats.damageDodged();
            dodgesCount += stats.dodgesCount();
            missesCount += stats.missesCount();
            totalPersonages++;
            if (!stats.isDead()) {
                remainPersonages++;
            }
        }

        private GroupBattleStats toStats() {
            return new GroupBattleStats(
                remainHealth,
                totalHealth,
                normalDamageDealt,
                normalAttackCount,
                critDamageDealt,
                critsCount,
                damageBlocked,
                blockCount,
                damageDodged,
                dodgesCount,
                missesCount,
                totalPersonages,
                remainPersonages
            );
        }
    }
}
