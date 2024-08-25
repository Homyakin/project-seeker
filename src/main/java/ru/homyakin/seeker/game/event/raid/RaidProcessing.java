package ru.homyakin.seeker.game.event.raid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.battle.two_team.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.battle.two_team.TwoTeamBattleWinner;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
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
    private final RaidDao raidDao;
    private final ItemService itemService;
    private final RaidGenerator raidGenerator;

    public RaidProcessing(
        PersonageService personageService,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle,
        RaidDao raidDao,
        ItemService itemService,
        RaidGenerator raidGenerator
    ) {
        this.personageService = personageService;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
        this.raidDao = raidDao;
        this.itemService = itemService;
        this.raidGenerator = raidGenerator;
    }

    public EventResult.Raid process(Event event, LaunchedEvent launchedEvent) {
        final var raid = raidDao.getByEventId(event.id())
            .orElseThrow(() -> new IllegalStateException("Raid must be present"));

        final var participants = personageService.getByLaunchedEvent(launchedEvent.id());
        if (participants.isEmpty()) {
            return new EventResult.Raid(
                EventResult.Raid.Status.EXPIRED,
                List.of(),
                List.of(),
                List.of()
            );
        }

        final var personages = participants.stream().map(Personage::toBattlePersonage).toList();
        final var result = twoPersonageTeamsBattle.battle(
            raidGenerator.generate(raid, launchedEvent, personages),
            personages
        );
        boolean doesParticipantsWin = result.winner() == TwoTeamBattleWinner.SECOND_TEAM;

        final var generatedItems = new ArrayList<GeneratedItemResult>();

        final var endTime = TimeUtils.moscowTime();
        final var raidResults = result.secondTeamResults().stream()
            .map(battleResult -> {
                final var reward = new Money(calculateReward(doesParticipantsWin, battleResult));
                personageService.addMoneyAndReduceEnergyForEvent(
                    battleResult.personage(),
                    reward,
                    endTime
                );
                final var generatedItem = doesParticipantsWin
                    ? generateItem(battleResult.personage())
                    : Optional.<GeneratedItemResult>empty();
                generatedItem.ifPresent(generatedItems::add);
                return new PersonageRaidResult(
                    battleResult.personage(),
                    battleResult.stats(),
                    reward,
                    generatedItem
                        .filter(it -> it instanceof GeneratedItemResult.Success)
                        .map(it -> ((GeneratedItemResult.Success) it).item())
                );
            })
            .toList();

        final var raidResult = new EventResult.Raid(
            doesParticipantsWin ? EventResult.Raid.Status.SUCCESS : EventResult.Raid.Status.FAILURE,
            result.firstTeamResults(),
            raidResults,
            generatedItems
        );
        personageService.saveRaidResults(raidResult.personageResults(), launchedEvent);
        return raidResult;
    }

    /**
     * Считает награду за рейд.
     * В случае поражения - награда равна базовой, в случае победы - награда зависит от нанесённого и полученного урона.
     * Бонус за урон считается по формуле log(1.1, урон / 10) - 43. При 1000 бонус примерно равен 5, при 3000 - 16
     */
    private int calculateReward(boolean doesParticipantsWin, PersonageBattleResult result) {
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
        Personage personage
    ) {
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
