package ru.homyakin.seeker.test_utils;

import java.util.Optional;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

public class DuelUtils {
    public static Duel withPersonages(PersonageId initiator, PersonageId acceptor) {
        return new Duel(
            TestRandom.nextLong(),
            initiator,
            acceptor,
            Optional.empty(),
            TimeUtils.moscowTime(),
            EventStatus.LAUNCHED
        );
    }
}
