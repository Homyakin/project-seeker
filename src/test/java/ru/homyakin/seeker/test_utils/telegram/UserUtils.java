package ru.homyakin.seeker.test_utils.telegram;

import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.Optional;

public class UserUtils {
    public static User randomUser() {
        return new User(
            new UserId(RandomUtils.nextLong()),
            true,
            Language.RU,
            new PersonageId(RandomUtils.nextLong()),
            Optional.empty()
        );
    }
}
