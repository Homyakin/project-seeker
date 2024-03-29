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
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.GenerateItemError;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.event.raid.models.RaidResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class RaidProcessing {
    private static final Logger logger = LoggerFactory.getLogger(RaidProcessing.class);
    private final PersonageService personageService;
    private final TwoPersonageTeamsBattle twoPersonageTeamsBattle;
    private final RaidDao raidDao;
    private final ItemService itemService;

    public RaidProcessing(
        PersonageService personageService,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle,
        RaidDao raidDao, ItemService itemService
    ) {
        this.personageService = personageService;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
        this.raidDao = raidDao;
        this.itemService = itemService;
    }

    public RaidResult process(Event event, List<Personage> participants) {
        final var raid = raidDao.getByEventId(event.id())
            .orElseThrow(() -> new IllegalStateException("Raid must be present"));

        final var result = twoPersonageTeamsBattle.battle(
            raid.template().generate(participants.size()).stream().map(Personage::toBattlePersonage).toList(),
            participants.stream().map(Personage::toBattlePersonageUsingEnergy).toList()
        );
        boolean doesParticipantsWin = result.winner() == TwoTeamBattleWinner.SECOND_TEAM;
        final var endTime = TimeUtils.moscowTime();
        final var raidResults = result.secondTeamResults().stream()
            .map(battleResult -> {
                final var reward = new Money(calculateReward(doesParticipantsWin, battleResult));
                personageService.addMoneyAndNullifyEnergy(
                    battleResult.personage(),
                    reward,
                    endTime
                );
                return new PersonageRaidResult(battleResult.personage(), battleResult.stats(), reward);
            })
            .toList();
        final var items = new ArrayList<GeneratedItemResult>();
        if (doesParticipantsWin) {
            generateItem(participants).ifPresent(items::add);
        }

        return new RaidResult(
            doesParticipantsWin,
            result.firstTeamResults(),
            raidResults,
            items
        );
    }

    private int calculateReward(boolean doesParticipantsWin, PersonageBattleResult result) {
        final int reward;
        if (!doesParticipantsWin) {
            reward = BASE_LOSE_REWARD;
        } else {
            reward = (int) (BASE_WIN_REWARD + result.stats().damageDealtAndTaken() / 200);
        }
        return Math.round(reward * result.personage().energy().percent());
    }

    private Optional<GeneratedItemResult> generateItem(List<Personage> personages) {
        final var resultChance = BASE_ITEM_GENERATE_CHANCE + personages.size() * 3;
        if (RandomUtils.processChance(resultChance)) {
            final var personage = RandomUtils.getRandomElement(personages);
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

    private static final int BASE_WIN_REWARD = 10;
    private static final int BASE_LOSE_REWARD = 5;
    private static final int BASE_ITEM_GENERATE_CHANCE = 5;
}
