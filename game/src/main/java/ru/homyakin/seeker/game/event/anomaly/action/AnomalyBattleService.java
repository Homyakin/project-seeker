package ru.homyakin.seeker.game.event.anomaly.action;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.database.GroupEventServiceDao;
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
import ru.homyakin.seeker.utils.TimeUtils;

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
    private final GroupEventServiceDao groupEventServiceDao;

    public AnomalyBattleService(
        PersonageEventService personageEventService,
        PersonageService personageService,
        EquipmentLoadoutService loadoutService,
        EventBattleLogService eventBattleLogService,
        LaunchedEventService launchedEventService,
        AnomalyConfig config,
        AnomalyGvgStorage gvgStorage,
        GroupEventServiceDao groupEventServiceDao
    ) {
        this.personageEventService = personageEventService;
        this.personageService = personageService;
        this.loadoutService = loadoutService;
        this.eventBattleLogService = eventBattleLogService;
        this.launchedEventService = launchedEventService;
        this.config = config;
        this.gvgStorage = gvgStorage;
        this.groupEventServiceDao = groupEventServiceDao;
    }

    public EventResult.AnomalyResult.BattleFinished fight(
        LaunchedEvent initiatorEvent,
        LaunchedEvent challengedEvent
    ) {
        final var initiatorParticipants = personageEventService.getParticipants(initiatorEvent.id());
        final var challengedParticipants = personageEventService.getParticipants(challengedEvent.id());

        final var initiatorTeam = toBattlePersonages(initiatorParticipants);
        final var challengedTeam = toBattlePersonages(challengedParticipants);
        final var battleResult = battle.process(initiatorTeam, challengedTeam);
        eventBattleLogService.save(initiatorEvent.id(), battleResult);
        eventBattleLogService.save(challengedEvent.id(), battleResult);

        final boolean initiatorWins = battleResult.firstWin();
        final var winnerEvent = initiatorWins ? initiatorEvent : challengedEvent;
        final var loserEvent = initiatorWins ? challengedEvent : initiatorEvent;
        final var winnerParticipants = initiatorWins ? initiatorParticipants : challengedParticipants;
        final var loserParticipants = initiatorWins ? challengedParticipants : initiatorParticipants;

        payRewards(winnerParticipants, config.victoryReward());
        payRewards(loserParticipants, config.defeatReward());

        final var initiatorGroupId = requireGroup(initiatorEvent.id());
        final var challengedGroupId = requireGroup(challengedEvent.id());
        updateElo(initiatorGroupId, challengedGroupId, initiatorWins);
        gvgStorage.saveRecentOpponent(initiatorGroupId, challengedGroupId, TimeUtils.moscowTime());

        launchedEventService.updateStatus(
            winnerEvent.id(),
            EventStatus.SUCCESS
        );
        launchedEventService.updateStatus(
            loserEvent.id(),
            EventStatus.FAILED
        );

        return new EventResult.AnomalyResult.BattleFinished(winnerEvent.id(), loserEvent.id());
    }

    private void updateElo(GroupId groupA, GroupId groupB, boolean aWins) {
        final var ratingA = gvgStorage.getRating(groupA);
        final var ratingB = gvgStorage.getRating(groupB);
        final double expectedA = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
        final double scoreA = aWins ? 1.0 : 0.0;
        final int deltaA = (int) Math.round(config.eloK() * (scoreA - expectedA));
        final int deltaB = -deltaA;
        gvgStorage.updateRating(groupA, Math.max(1, ratingA + deltaA));
        gvgStorage.updateRating(groupB, Math.max(1, ratingB + deltaB));
    }

    private void payRewards(List<EventParticipant> participants, Money reward) {
        for (final var participant : participants) {
            personageService.addMoney(participant.personage(), reward);
        }
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

    private GroupId requireGroup(long launchedEventId) {
        final var groups = groupEventServiceDao.getGroupsByLaunchedEventId(launchedEventId);
        if (groups.isEmpty()) {
            throw new IllegalStateException("Anomaly event without group: " + launchedEventId);
        }
        return groups.getFirst();
    }
}
