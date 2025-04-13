package ru.homyakin.seeker.test_utils.telegram;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.test_utils.TestRandom;

public class GroupUtils {
    public static GroupTg randomGroup() {
        return randomWithLanguage(Language.RU);
    }

    public static GroupTg randomWithLanguage(Language language) {
        return new GroupTg(
            new GroupTgId(TestRandom.nextLong()),
            language,
            new GroupId(TestRandom.nextLong())
        );
    }
}
