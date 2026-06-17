package ru.homyakin.seeker.test_utils.telegram;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.test_utils.TestRandom;

import java.util.Optional;

public class UserUtils {
    public static User randomUser() {
        return new User(
            new UserId(TestRandom.nextLong()),
            true,
            Language.RU,
            new PersonageId(TestRandom.nextLong()),
            Optional.empty()
        );
    }
}
