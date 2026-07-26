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
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.anomaly.generator.AnomalySafePveGenerator;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.EventParticipant;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
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
        final var participants = personageEventService.getParticipants(event.id());
        final var players = toBattlePersonages(participants);
        final var enemies = new AnomalySafePveGenerator().generate(safe.template(), players);
        final var battleResult = battle.process(enemies, players);
        eventBattleLogService.save(event.id(), battleResult);

        final boolean victory = !battleResult.firstWin();
        final var reward = victory ? config.victoryReward() : config.defeatReward();
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

        final var victoryReward = config.victoryReward();
        final var defeatReward = config.defeatReward();
        payRewards(winnerParticipants, victoryReward);
        payRewards(loserParticipants, defeatReward);
        updateElo(accepted.groupId(), opponentGroupId, initiatorWins);
        anomalyStorage.update(accepted.withWinner(winnerGroupId));
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

    private static List<AnomalyPersonageResult> toPersonageResults(
        List<EventParticipant> participants,
        List<BattlePersonage> team,
        BattleResult battleResult,
        Money reward
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

    private void payRewards(List<EventParticipant> participants, Money reward) {
        for (final var participant : participants) {
            personageService.addMoney(participant.personage(), reward);
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
                return BattlePersonage.forCombat(
                    gear.items(),
                    gear.battlePosition(),
                    personage.effects(),
                    Optional.of(LocaleUtils.personageNameWithBadge(personage))
                );
            })
            .toList();
    }
}
