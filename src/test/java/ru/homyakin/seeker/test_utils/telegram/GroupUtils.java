package ru.homyakin.seeker.test_utils.telegram;

import java.util.List;
import ru.homyakin.seeker.game.event.models.EventInterval;
import ru.homyakin.seeker.game.event.models.EventIntervals;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.GroupSettings;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.utils.TimeUtils;

public class GroupUtils {
    public static Group randomGroup() {
        return new Group(
            new GroupId(TestRandom.nextLong()),
            true,
            Language.RU,
            new GroupSettings(
                TimeUtils.moscowOffset(),
                new EventIntervals(
                    List.of(
                        new EventInterval(
                            0,
                            23,
                            true
                        )
                    )
                )
            )
        );
    }
}
