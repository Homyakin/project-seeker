package ru.homyakin.seeker.test_utils.telegram;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.test_utils.TestRandom;

public class GroupUtils {
    public static GroupTg randomGroup() {
        return new GroupTg(
            new GroupTgId(TestRandom.nextLong()),
            Language.RU,
            new GroupId(TestRandom.nextLong())
        );
    }
}
