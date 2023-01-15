package ru.homyakin.seeker.game.event.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class BossProcessing {
    private static final Logger logger = LoggerFactory.getLogger(BossProcessing.class);
    private final PersonageService personageService;
    private final TwoPersonageTeamsBattle twoPersonageTeamsBattle;

    public BossProcessing(
        PersonageService personageService,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle
    ) {
        this.personageService = personageService;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
    }

    public EventResult process(Event event, List<Personage> participants) {
        final var bossPersonage = personageService.getByBossEvent(event.id())
            .orElseThrow(() -> new IllegalStateException("Boss event must contain personage " + event.id()));

        final var personages = new ArrayList<BattlePersonage>(participants.size());
        final var idToPersonages = new HashMap<Long, BattlePersonage>();
        for (final var participant: participants) {
            final var personage = participant.toBattlePersonage();
            personages.add(personage);
            idToPersonages.put(participant.id(), personage);
        }

        final var result = twoPersonageTeamsBattle.battle(
            List.of(bossPersonage.toBattlePersonage()),
            personages
        );

        boolean doesParticipantsWin = result instanceof TwoPersonageTeamsBattle.Result.SecondTeamWin;
        final var endTime = TimeUtils.moscowTime();
        for (final var participant: participants) {
            final var personage = idToPersonages.get(participant.id());
            if (personage == null) {
                logger.error("Personage with id {} is missing in battle map", participant.id());
                continue;
            }
            personageService.addExperienceAndChangeHealth(
                participant,
                calculateExperience(personage, doesParticipantsWin),
                personage.health(),
                endTime
            );
        }

        if (doesParticipantsWin) {
            return new EventResult.Success();
        } else {
            return new EventResult.Failure();
        }
    }

    private long calculateExperience(BattlePersonage personage, boolean isWin) {
        double exp = (double) personage.damageDealtAndTaken() / 20;
        logger.debug("Planning exp for personage {} is {}", personage.id(), exp);
        if (isWin) {
            exp = Math.max(2, exp * WIN_MULTIPLIER);
        } else {
            exp = Math.max(1, exp * LOSE_MULTIPLIER);
        }
        return (long) exp;
    }

    private static final double WIN_MULTIPLIER = 2;
    private static final double LOSE_MULTIPLIER = 1;
}
