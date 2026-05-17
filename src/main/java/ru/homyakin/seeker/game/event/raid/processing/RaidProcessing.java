package ru.homyakin.seeker.game.event.raid.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.v3.two_team.PersonageBattleResult;
import ru.homyakin.seeker.game.battle.v3.two_team.PersonageBattleStats;
import ru.homyakin.seeker.game.battle.v4.Battle;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.BattlePersonageStats;
import ru.homyakin.seeker.game.battle.v4.Position;
import ru.homyakin.seeker.game.event.models.EventResult;
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
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.outpost.action.GroupPassiveEffectsService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class RaidProcessing {
    private static final Logger logger = LoggerFactory.getLogger(RaidProcessing.class);
    private final PersonageService personageService;
    private final ItemService itemService;
    private final Battle battle = new Battle();
    private final RaidService raidService;
    private final RaidItemGenerator raidItemGenerator;
    private final RaidGenerator raidGenerator;
    private final RaidRewardGenerator raidRewardGenerator;
    private final LaunchedEventService launchedEventService;
    private final PersonageEventService personageEventService;
    private final GroupEventService groupEventService;
    private final GroupPassiveEffectsService groupPassiveEffectsService;

    public RaidProcessing(
        PersonageService personageService,
        ItemService itemService,
        RaidService raidService,
        RaidItemGenerator raidItemGenerator,
        RaidGenerator raidGenerator,
        RaidRewardGenerator raidRewardGenerator,
        LaunchedEventService launchedEventService,
        PersonageEventService personageEventService,
        GroupEventService groupEventService,
        GroupPassiveEffectsService groupPassiveEffectsService
    ) {
        this.personageService = personageService;
        this.itemService = itemService;
        this.raidService = raidService;
        this.raidItemGenerator = raidItemGenerator;
        this.raidGenerator = raidGenerator;
        this.raidRewardGenerator = raidRewardGenerator;
        this.launchedEventService = launchedEventService;
        this.personageEventService = personageEventService;
        this.groupEventService = groupEventService;
        this.groupPassiveEffectsService = groupPassiveEffectsService;
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
        final var enemies = raidGenerator.generate(RaidType.WOLFPACK, raidEvent, personages);
        final var initialHealth = initialHealth(enemies, personages);
        final var result = battle.process(enemies, personages);
        boolean doesParticipantsWin = !result.firstWin();

        final var generatedItems = new ArrayList<GeneratedItemResult>();

        final var raidResults = new ArrayList<PersonageRaidResult>();
        for (int i = 0; i < participants.size(); i++) {
            final var participant = participants.get(i);
            final var battlePersonage = personages.get(i);
            final var battleResult = toBattleResult(
                participant.personage(),
                result.personageStats().get(battlePersonage.id()),
                initialHealth.get(battlePersonage.id())
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
                result.personageStats().get(enemy.id()),
                initialHealth.get(enemy.id())
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
        final var equippedItemsByPersonageId = itemService.getEquippedItemsByPersonageIds(
            participants.stream()
                .map(participant -> participant.personage().id())
                .collect(Collectors.toSet())
        );
        return participants.stream()
            .map(participant -> new BattlePersonage(
                equippedItemsByPersonageId.getOrDefault(participant.personage().id(), List.of()),
                Position.FRONT
            ))
            .toList();
    }

    private Map<UUID, Integer> initialHealth(List<BattlePersonage> enemies, List<BattlePersonage> personages) {
        final var initialHealth = new HashMap<UUID, Integer>();
        enemies.forEach(personage -> initialHealth.put(personage.id(), personage.health()));
        personages.forEach(personage -> initialHealth.put(personage.id(), personage.health()));
        return initialHealth;
    }

    private PersonageBattleResult toBattleResult(
        Personage personage,
        BattlePersonageStats stats,
        int maxHealth
    ) {
        return new PersonageBattleResult(
            personage,
            new PersonageBattleStats(
                stats.remainHealth(),
                stats.normalDamageDealt(),
                stats.normalAttackCount(),
                stats.critDamageDealt(),
                stats.critsCount(),
                stats.damageBlocked(),
                stats.blockCount(),
                stats.damageDodged(),
                stats.dodgesCount(),
                stats.missesCount(),
                new Characteristics(maxHealth, 0, 0)
            )
        );
    }

    private int calculateRaidPoints(int raidLevel, boolean isWin) {
        if (isWin) {
            return 2 * raidLevel;
        } else {
            return raidLevel;
        }
    }
}
