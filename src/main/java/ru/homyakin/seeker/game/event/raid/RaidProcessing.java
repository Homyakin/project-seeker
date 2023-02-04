package ru.homyakin.seeker.game.event.raid;

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

    public EventResult process(Event event, List<Personage> participants) {
        final var raid = raidDao.getByEventId(event.id())
            .orElseThrow(() -> new IllegalStateException("Raid must be present"));
        final var personages = new ArrayList<BattlePersonage>(participants.size());
        final var idToPersonages = new HashMap<Long, BattlePersonage>();
        for (final var participant: participants) {
            final var personage = participant.toBattlePersonage();
            personages.add(personage);
            idToPersonages.put(participant.id(), personage);
        }

        final var result = twoPersonageTeamsBattle.battle(
            raid.template().generate(),
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
            personageService.changeHealth(
                participant,
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
}
