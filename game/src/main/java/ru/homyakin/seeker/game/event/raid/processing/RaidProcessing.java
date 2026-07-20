package ru.homyakin.seeker.game.event.raid.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.result.PersonageBattleResult;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.effect.ItemFoundChanceBonus;
import ru.homyakin.seeker.game.effect.RaidGoldRewardBonus;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.outpost.action.GroupPassiveEffectsService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class RaidProcessing {
    private static final Logger logger = LoggerFactory.getLogger(RaidProcessing.class);
    private final PersonageService personageService;
    private final EquipmentLoadoutService loadoutService;
    private final Battle battle = new Battle();
    private final RaidService raidService;
    private final RaidItemGenerator raidItemGenerator;
    private final RaidGenerator raidGenerator;
    private final RaidRewardGenerator raidRewardGenerator;
    private final LaunchedEventService launchedEventService;
    private final PersonageEventService personageEventService;
    private final GroupEventService groupEventService;
    private final GroupPassiveEffectsService groupPassiveEffectsService;
    private final EventBattleLogService eventBattleLogService;

    public RaidProcessing(
        PersonageService personageService,
        EquipmentLoadoutService loadoutService,
        RaidService raidService,
        RaidItemGenerator raidItemGenerator,
        RaidGenerator raidGenerator,
        RaidRewardGenerator raidRewardGenerator,
        LaunchedEventService launchedEventService,
        PersonageEventService personageEventService,
        GroupEventService groupEventService,
        GroupPassiveEffectsService groupPassiveEffectsService,
        EventBattleLogService eventBattleLogService
    ) {
        this.personageService = personageService;
        this.loadoutService = loadoutService;
        this.raidService = raidService;
        this.raidItemGenerator = raidItemGenerator;
        this.raidGenerator = raidGenerator;
        this.raidRewardGenerator = raidRewardGenerator;
        this.launchedEventService = launchedEventService;
        this.personageEventService = personageEventService;
        this.groupEventService = groupEventService;
        this.groupPassiveEffectsService = groupPassiveEffectsService;
        this.eventBattleLogService = eventBattleLogService;
    }

    public EventResult.RaidResult process(LaunchedEvent launchedEvent) {
        final var raid = raidService.getByEventId(launchedEvent.eventId())
            .orElseThrow(() -> new IllegalStateException("Raid must be present"));
        final var raidEvent = LaunchedRaidEvent.fromLaunchedEvent(launchedEvent);

        final var participants = personageEventService.getRaidParticipants(launchedEvent.id());
        if (participants.isEmpty()) {
            logger.info("Raid {} is expired", launchedEvent.id());
            final var result = EventResult.RaidResult.Expired.INSTANCE;
            launchedEventService.updateResult(raidEvent, result);
            return result;
        }

        final var now = TimeUtils.moscowTime();
        final var groupPassiveEffectsCache = new HashMap<GroupId, List<GroupPassiveEffect>>();
        final var personages = toBattlePersonages(participants);
        final var enemies = raidGenerator.generate(RaidType.fromCode(raid.code()), raidEvent, personages);
        final var result = battle.process(enemies, personages);
        eventBattleLogService.save(launchedEvent.id(), result);
        boolean doesParticipantsWin = !result.firstWin();

        final var generatedItems = new ArrayList<GeneratedItemResult>();

        final var raidResults = new ArrayList<PersonageRaidResult>();
        for (int i = 0; i < participants.size(); i++) {
            final var participant = participants.get(i);
            final var battlePersonage = personages.get(i);
            final var battleResult = toBattleResult(
                participant.personage(),
                result.personageStats().get(battlePersonage.id())
            );
            final var personage = participant.personage();
            final int personageRaidGoldPercent = RaidGoldRewardBonus.sumPersonageEffects(personage.effects(), now);
            final int personageItemFoundBonusPercent = ItemFoundChanceBonus.sumPersonageEffects(
                personage.effects(),
                now
            );
            final var groupPassives = personage.memberGroupId()
                .map(gid -> groupPassiveEffectsCache.computeIfAbsent(
                    gid,
                    groupPassiveEffectsService::listPassiveEffects
                ))
                .orElse(Collections.emptyList());
            final int groupRaidGoldPercent = RaidGoldRewardBonus.sumGroupPassiveEffects(groupPassives, now);
            final int groupItemFoundBonusPercent = ItemFoundChanceBonus.sumGroupPassiveEffects(groupPassives, now);
            final var reward = new Money(
                raidRewardGenerator.calculateReward(
                    doesParticipantsWin,
                    battleResult,
                    participant.params().isExhausted(),
                    personageRaidGoldPercent + groupRaidGoldPercent
                )
            );
            personageService.addMoney(
                participant.personage(),
                reward
            );
            final var generatedItem = raidItemGenerator.generateItem(
                doesParticipantsWin,
                participant.personage(),
                participant.params().isExhausted(),
                raidEvent.raidParams().raidLevel(),
                personageItemFoundBonusPercent + groupItemFoundBonusPercent
            );
            generatedItem.ifPresent(generatedItems::add);
            raidResults.add(new PersonageRaidResult(
                participant,
                battleResult.stats(),
                reward,
                generatedItem.map(GeneratedItemResult::toRaidItem)
            ));
        }

        final var raidNpcResults = enemies.stream()
            .map(enemy -> toBattleResult(
                null,
                result.personageStats().get(enemy.id())
            ))
            .toList();

        final var raidResult = new EventResult.RaidResult.Completed(
            doesParticipantsWin
                ? EventResult.RaidResult.Completed.Status.SUCCESS
                : EventResult.RaidResult.Completed.Status.FAILURE,
            raid,
            raidEvent,
            raidNpcResults,
            raidResults,
            generatedItems,
            calculateRaidPoints(raidEvent.raidParams().raidLevel(), doesParticipantsWin)
        );
        personageService.saveRaidResults(raidResult.personageResults(), launchedEvent);
        groupEventService.updateRaidLevel(launchedEvent.id(), doesParticipantsWin);
        launchedEventService.updateResult(raidEvent, raidResult);
        logger.info("Raid {} status is {}", launchedEvent.id(), raidResult.status());
        return raidResult;
    }

    private List<BattlePersonage> toBattlePersonages(List<RaidParticipant> participants) {
        final var personages = participants.stream()
            .map(RaidParticipant::personage)
            .toList();
        final var combatGearByPersonageId = loadoutService.resolveCombatGear(personages, EventType.RAID);
        return participants.stream()
            .map(participant -> {
                final var personage = participant.personage();
                final var combatGear = combatGearByPersonageId.get(personage.id());
                return BattlePersonage.forCombat(
                    combatGear.items(),
                    combatGear.battlePosition(),
                    personage.effects(),
                    Optional.of(LocaleUtils.personageNameWithBadge(personage))
                );
            })
            .toList();
    }

    private PersonageBattleResult toBattleResult(
        Personage personage,
        BattlePersonageStats stats
    ) {
        return new PersonageBattleResult(personage, stats);
    }

    private int calculateRaidPoints(int raidLevel, boolean isWin) {
        if (isWin) {
            return 2 * raidLevel;
        } else {
            return raidLevel;
        }
    }
}
