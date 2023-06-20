package ru.homyakin.seeker.game.event.raid;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.two_team.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.battle.two_team.TwoTeamBattleWinner;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
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
            raid.template().generate(participants.size()),
            participants
        );

        boolean doesParticipantsWin = result.winner() == TwoTeamBattleWinner.SECOND_TEAM;
        int baseReward = doesParticipantsWin ? 10 : 2; // TODO баланс
        final var endTime = TimeUtils.moscowTime();
        for (final var personageResult: result.secondTeamResult().personageResults()) {
            personageService.addMoney(
                personageResult.personage(),
                new Money((int) (
                    baseReward + Math.sqrt(
                        (double) personageResult.battlePersonage().battleStats().damageDealtAndBlocked() / 10
                    )
                ))
            );
        }

        return new RaidResult(
            doesParticipantsWin,
            result.firstTeamResult().personageResults(),
            result.secondTeamResult().personageResults()
        );
    }
}
