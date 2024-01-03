package ru.homyakin.seeker.game.event.raid;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.battle.two_team.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.battle.two_team.TwoTeamBattleWinner;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.PersonageRaidResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class RaidProcessing {
    private static final Logger logger = LoggerFactory.getLogger(RaidProcessing.class);
    private final PersonageService personageService;
    private final TwoPersonageTeamsBattle twoPersonageTeamsBattle;
    private final RaidDao raidDao;

    public RaidProcessing(
        PersonageService personageService,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle,
        RaidDao raidDao
    ) {
        this.personageService = personageService;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
        this.raidDao = raidDao;
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

        return new RaidResult(
            doesParticipantsWin,
            result.firstTeamResults(),
            raidResults
        );
    }

    private int calculateReward(boolean doesParticipantsWin, PersonageBattleResult result) {
        final int reward;
        if (!doesParticipantsWin) {
            reward = BASE_LOSE_REWARD;
        } else {
            // За рейд где-то 300-500 урона и столько же получено; log3.4(700) ~ 5.4;
            reward = (int) Math.round(
                BASE_WIN_REWARD + MathUtils.log(3.4, result.stats().damageDealtAndTaken())
            );
        }
        return Math.round(reward * result.personage().energy().percent());
    }

    private static final int BASE_WIN_REWARD = 10;
    private static final int BASE_LOSE_REWARD = 5;
}
