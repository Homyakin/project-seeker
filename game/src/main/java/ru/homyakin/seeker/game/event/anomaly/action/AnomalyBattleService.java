package ru.homyakin.seeker.game.event.anomaly.action;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleResult;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPersonageResult;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyReward;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.anomaly.generator.AnomalySafePveGenerator;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.EventParticipant;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.locale.LocaleUtils;

@Service
public class AnomalyBattleService {
    private final PersonageEventService personageEventService;
    private final PersonageService personageService;
    private final EquipmentLoadoutService loadoutService;
    private final Battle battle = new Battle();
    private final EventBattleLogService eventBattleLogService;
    private final LaunchedEventService launchedEventService;
    private final AnomalyConfig config;
    private final AnomalyGvgStorage gvgStorage;
    private final AnomalyStorage anomalyStorage;

    public AnomalyBattleService(
        PersonageEventService personageEventService,
        PersonageService personageService,
        EquipmentLoadoutService loadoutService,
        EventBattleLogService eventBattleLogService,
        LaunchedEventService launchedEventService,
        AnomalyConfig config,
        AnomalyGvgStorage gvgStorage,
        AnomalyStorage anomalyStorage
    ) {
        this.personageEventService = personageEventService;
        this.personageService = personageService;
        this.loadoutService = loadoutService;
        this.eventBattleLogService = eventBattleLogService;
        this.launchedEventService = launchedEventService;
        this.config = config;
        this.gvgStorage = gvgStorage;
        this.anomalyStorage = anomalyStorage;
    }

    public EventResult.AnomalyResult.PveBattleFinished fightPve(LaunchedEvent event, Anomaly.Safe safe) {
        return fightPve(
            event,
            safe.groupId(),
            Optional.empty(),
            safe.template(),
            personageEventService.getParticipants(event.id()),
            true
        );
    }

    public EventResult.AnomalyResult.PveBattleFinished fightPveFallback(
        LaunchedEvent event,
        GroupId initiatorGroupId,
        Optional<GroupId> failedOpponentGroupId
    ) {
        final var initiatorParticipants = participantsOfGroup(
            personageEventService.getParticipants(event.id()),
            initiatorGroupId
        );
        return fightPve(
            event,
            initiatorGroupId,
            failedOpponentGroupId,
            AnomalyPveTemplate.random(),
            initiatorParticipants,
            false
        );
    }

    private EventResult.AnomalyResult.PveBattleFinished fightPve(
        LaunchedEvent event,
        GroupId initiatorGroupId,
        Optional<GroupId> failedOpponentGroupId,
        AnomalyPveTemplate template,
        List<EventParticipant> participants,
        boolean safePve
    ) {
        final var players = toBattlePersonages(participants);
        final var enemies = new AnomalySafePveGenerator().generate(template, players);
        final var battleResult = battle.process(enemies, players);
        eventBattleLogService.save(event.id(), battleResult);

        final boolean victory = !battleResult.firstWin();
        final var reward = pveReward(safePve, victory);
        payRewards(participants, reward);
        launchedEventService.updateStatus(event.id(), victory ? EventStatus.SUCCESS : EventStatus.FAILED);

        final var personageResults = new java.util.ArrayList<AnomalyPersonageResult>(participants.size());
        for (int i = 0; i < participants.size(); i++) {
            personageResults.add(new AnomalyPersonageResult(
                participants.get(i).personage(),
                battleResult.personageStats().get(players.get(i).id()),
                reward
            ));
        }
        final var enemyStats = enemies.stream()
            .map(enemy -> battleResult.personageStats().get(enemy.id()))
            .toList();
        personageService.saveAnomalyResults(personageResults, event.id());
        return new EventResult.AnomalyResult.PveBattleFinished(
            event.id(),
            initiatorGroupId,
            failedOpponentGroupId,
            victory,
            reward,
            personageResults,
            enemyStats
        );
    }

    public EventResult.AnomalyResult.BattleFinished fight(
        LaunchedEvent event,
        Anomaly.Dangerous.Accepted accepted
    ) {
        final var opponentGroupId = accepted.opponentGroupId();
        final var allParticipants = personageEventService.getParticipants(event.id());
        final var initiatorParticipants = participantsOfGroup(allParticipants, accepted.groupId());
        final var challengedParticipants = participantsOfGroup(allParticipants, opponentGroupId);

        final var initiatorTeam = toBattlePersonages(initiatorParticipants);
        final var challengedTeam = toBattlePersonages(challengedParticipants);
        final var battleResult = battle.process(initiatorTeam, challengedTeam);
        eventBattleLogService.save(event.id(), battleResult);

        final boolean initiatorWins = battleResult.firstWin();
        final var winnerGroupId = initiatorWins ? accepted.groupId() : opponentGroupId;
        final var loserGroupId = initiatorWins ? opponentGroupId : accepted.groupId();
        final var winnerParticipants = initiatorWins ? initiatorParticipants : challengedParticipants;
        final var loserParticipants = initiatorWins ? challengedParticipants : initiatorParticipants;

        final var victoryReward = config.gvgWinReward();
        final var defeatReward = config.gvgLoseReward();
        payRewards(winnerParticipants, victoryReward);
        payRewards(loserParticipants, defeatReward);
        updateElo(accepted.groupId(), opponentGroupId, initiatorWins);
        final var finished = accepted.withWinner(winnerGroupId);
        anomalyStorage.update(finished);
        accepted.opponentLaunchedEventId().ifPresent(opponentEventId ->
            anomalyStorage.findByLaunchedEventId(opponentEventId).ifPresent(opponent -> {
                if (opponent instanceof Anomaly.Dangerous.Accepted guestAccepted) {
                    anomalyStorage.update(guestAccepted.withWinner(winnerGroupId));
                }
            })
        );
        launchedEventService.updateStatus(event.id(), EventStatus.SUCCESS);

        final var winnerResults = toPersonageResults(
            initiatorWins ? initiatorParticipants : challengedParticipants,
            initiatorWins ? initiatorTeam : challengedTeam,
            battleResult,
            victoryReward
        );
        final var loserResults = toPersonageResults(
            initiatorWins ? challengedParticipants : initiatorParticipants,
            initiatorWins ? challengedTeam : initiatorTeam,
            battleResult,
            defeatReward
        );
        personageService.saveAnomalyResults(
            java.util.stream.Stream.concat(winnerResults.stream(), loserResults.stream()).toList(),
            event.id()
        );
        return new EventResult.AnomalyResult.BattleFinished(
            event.id(),
            winnerGroupId,
            loserGroupId,
            winnerResults,
            loserResults
        );
    }

    private AnomalyReward pveReward(boolean safePve, boolean victory) {
        if (safePve) {
            return victory ? config.pveWinReward() : config.pveLoseReward();
        }
        return victory ? config.gvgFallbackWinReward() : config.gvgFallbackLoseReward();
    }

    private static List<AnomalyPersonageResult> toPersonageResults(
        List<EventParticipant> participants,
        List<BattlePersonage> team,
        BattleResult battleResult,
        AnomalyReward reward
    ) {
        final var results = new java.util.ArrayList<AnomalyPersonageResult>(participants.size());
        for (int i = 0; i < participants.size(); i++) {
            results.add(new AnomalyPersonageResult(
                participants.get(i).personage(),
                battleResult.personageStats().get(team.get(i).id()),
                reward
            ));
        }
        return results;
    }

    private void updateElo(GroupId groupA, GroupId groupB, boolean aWins) {
        final var ratingA = gvgStorage.getRating(groupA);
        final var ratingB = gvgStorage.getRating(groupB);
        final double expectedA = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
        final double scoreA = aWins ? 1.0 : 0.0;
        final int deltaA = (int) Math.round(config.eloK() * (scoreA - expectedA));
        gvgStorage.updateRating(groupA, Math.max(1, ratingA + deltaA));
        gvgStorage.updateRating(groupB, Math.max(1, ratingB - deltaA));
    }

    private void payRewards(List<EventParticipant> participants, AnomalyReward reward) {
        final var moneyMap = participants.stream()
            .collect(java.util.stream.Collectors.toMap(
                participant -> participant.personage().id(),
                _ -> reward.money(),
                (a, _) -> a
            ));
        personageService.addMoneyBatch(moneyMap);
        if (!reward.stormShards().isZero()) {
            final var shardsMap = participants.stream()
                .collect(java.util.stream.Collectors.toMap(
                    participant -> participant.personage().id(),
                    _ -> reward.stormShards(),
                    (a, _) -> a
                ));
            personageService.addStormShardsBatch(shardsMap);
        }
    }

    private static List<EventParticipant> participantsOfGroup(
        List<EventParticipant> participants,
        GroupId groupId
    ) {
        return participants.stream()
            .filter(participant -> participant.personage().memberGroupId()
                .filter(groupId::equals)
                .isPresent())
            .toList();
    }

    private List<BattlePersonage> toBattlePersonages(List<EventParticipant> participants) {
        final var personages = participants.stream().map(EventParticipant::personage).toList();
        final var combatGear = loadoutService.resolveCombatGear(personages, EventType.ANOMALY);
        return participants.stream()
            .map(participant -> {
                final var personage = participant.personage();
                final var gear = combatGear.get(personage.id());
                // Storm enhance does not work in anomaly
                final var items = gear.items().stream()
                    .map(item -> item.withoutStormEnhance())
                    .toList();
                return BattlePersonage.forCombat(
                    items,
                    gear.battlePosition(),
                    PersonageEffects.EMPTY,
                    Optional.of(LocaleUtils.personageNameWithBadge(personage))
                );
            })
            .toList();
    }
}
