package ru.homyakin.seeker.test_utils.telegram;

import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.ActiveTime;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public class GroupUtils {
    public static Group randomGroup() {
        return new Group(
            new GroupId(RandomUtils.nextLong()),
            true,
            Language.RU,
            ActiveTime.createDefault()
        );
    }
}
