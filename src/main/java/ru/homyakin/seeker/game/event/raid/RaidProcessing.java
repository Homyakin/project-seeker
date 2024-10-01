package ru.homyakin.seeker.game.event.raid;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.battle.two_team.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.battle.two_team.TwoTeamBattleWinner;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class RaidProcessing {
    private static final Logger logger = LoggerFactory.getLogger(RaidProcessing.class);
    private final PersonageService personageService;
    private final TwoPersonageTeamsBattle twoPersonageTeamsBattle;
    private final RaidService raidService;
    private final ItemService itemService;
    private final RaidGenerator raidGenerator;
    private final LaunchedEventService launchedEventService;
    private final PersonageEventService personageEventService;

    public RaidProcessing(
        PersonageService personageService,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle,
        RaidService raidService,
        ItemService itemService,
        RaidGenerator raidGenerator,
        LaunchedEventService launchedEventService,
        PersonageEventService personageEventService
    ) {
        this.personageService = personageService;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
        this.raidService = raidService;
        this.itemService = itemService;
        this.raidGenerator = raidGenerator;
        this.launchedEventService = launchedEventService;
        this.personageEventService = personageEventService;
    }

    public EventResult.RaidResult process(LaunchedEvent launchedEvent) {
        final var raid = raidService.getByEventId(launchedEvent.eventId())
            .orElseThrow(() -> new IllegalStateException("Raid must be present"));

        final var participants = personageEventService.getRaidParticipants(launchedEvent.id());
        if (participants.isEmpty()) {
            logger.info("Raid {} is expired", launchedEvent.id());
            final var result = EventResult.RaidResult.Expired.INSTANCE;
            launchedEventService.updateResult(launchedEvent, result);
            return result;
        }
        final var idToParticipant = participants.stream().collect(Collectors.toMap(it -> it.personage().id(), it -> it));
        final var personages = participants.stream().map(RaidParticipant::personage).map(Personage::toBattlePersonage).toList();
        final var result = twoPersonageTeamsBattle.battle(
            raidGenerator.generate(raid, launchedEvent, personages),
            personages
        );
        boolean doesParticipantsWin = result.winner() == TwoTeamBattleWinner.SECOND_TEAM;

        final var generatedItems = new ArrayList<GeneratedItemResult>();

        final var endTime = TimeUtils.moscowTime();
        final var raidResults = result.secondTeamResults().stream()
            .map(battleResult -> {
                final var participant = idToParticipant.get(battleResult.personage().id());
                final var reward = new Money(
                    calculateReward(
                        doesParticipantsWin,
                        battleResult,
                        participant.params().isExhausted()
                    )
                );
                personageService.addMoney(
                    participant.personage(),
                    reward,
                    endTime
                );
                final var generatedItem = doesParticipantsWin
                    ? generateItem(battleResult.personage(), participant.params().isExhausted())
                    : Optional.<GeneratedItemResult>empty();
                generatedItem.ifPresent(generatedItems::add);
                return new PersonageRaidResult(
                    participant,
                    battleResult.stats(),
                    reward,
                    generatedItem
                        .filter(it -> it instanceof GeneratedItemResult.Success)
                        .map(it -> ((GeneratedItemResult.Success) it).item())
                );
            })
            .toList();

        final var raidResult = new EventResult.RaidResult.Completed(
            doesParticipantsWin ? EventResult.RaidResult.Completed.Status.SUCCESS : EventResult.RaidResult.Completed.Status.FAILURE,
            raid,
            result.firstTeamResults(),
            raidResults,
            generatedItems
        );
        personageService.saveRaidResults(raidResult.personageResults(), launchedEvent);
        launchedEventService.updateResult(launchedEvent, raidResult);
        logger.info("Raid {} status is {}", launchedEvent.id(), raidResult.status());
        return raidResult;
    }

    /**
     * Считает награду за рейд.
     * В случае поражения - награда равна базовой, в случае победы - награда зависит от нанесённого и полученного урона.
     * Бонус за урон считается по формуле log(1.1, урон / 10) - 43. При 1000 бонус примерно равен 5, при 3000 - 16
     */
    private int calculateReward(boolean doesParticipantsWin, PersonageBattleResult result, boolean isExhausted) {
        if (isExhausted) {
            return 0;
        }
        final int reward;
        if (!doesParticipantsWin) {
            reward = BASE_REWARD;
        } else {
            var bonusMoney = MathUtils.log(1.1, (double) result.stats().damageDealtAndTaken() / 10) - 43;
            if (bonusMoney < 0) {
                bonusMoney = 0;
            }
            reward = (int) Math.round(BASE_REWARD + bonusMoney);
        }
        return reward;
    }

    /**
     * Функция генерации предметов для персонажа
     * В основе лежит функция:
     * Если x <= 5 => y = 2*x (нужно, потому что степенные функции в начале растут очень медленно)
     * Если x > 5 => y = 10 + ((x - 5)^2) / 2.5
     * x - количество рейдов подряд без предметов
     * y - вероятность получить предмет в процентах
     */
    private Optional<GeneratedItemResult> generateItem(
        Personage personage,
        boolean isExhausted
    ) {
        if (isExhausted) {
            return Optional.empty();
        }
        final var raidsWithoutItems = personageService.countSuccessRaidsFromLastItem(personage.id());
        final int chance;
        if (raidsWithoutItems <= 5) {
            chance = raidsWithoutItems * 2;
        } else {
            chance = (int) (10 + Math.pow(raidsWithoutItems - 5, 2) / 2.5);
        }
        if (RandomUtils.processChance(chance)) {
            final var result = itemService.generateItemForPersonage(personage)
                .fold(
                    error -> switch (error) {
                        case GenerateItemError.NotEnoughSpace notEnoughSpace ->
                            new GeneratedItemResult.NotEnoughSpaceInBag(personage, notEnoughSpace.item());
                    },
                    item -> new GeneratedItemResult.Success(personage, item)
                );
            return Optional.of(result);
        }
        return Optional.empty();
    }

    private static final int BASE_REWARD = 5;
}
