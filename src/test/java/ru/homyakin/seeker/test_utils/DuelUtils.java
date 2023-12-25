package ru.homyakin.seeker.test_utils;

import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

public class DuelUtils {
    public static Duel withPersonages(PersonageId initiator, PersonageId acceptor) {
        return new Duel(
            RandomUtils.nextLong(),
            initiator,
            acceptor,
            TimeUtils.moscowTime(),
            DuelStatus.WAITING
        );
    }
}
