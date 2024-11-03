package ru.homyakin.seeker.game.top;

import java.util.Comparator;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.top.models.GroupTopRaidPosition;
import ru.homyakin.seeker.game.top.models.GroupTopRaidResult;
import ru.homyakin.seeker.game.top.models.PersonageTopPosition;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.game.top.models.TopSpinResult;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class TopService {
    private final TopDao topDao;

    public TopService(TopDao topDao) {
        this.topDao = topDao;
    }

    public TopRaidResult getTopRaidWeek() {
        final var start = TimeUtils.thisWeekMonday();
        final var end = TimeUtils.thisWeekSunday();
        final var top = topDao.getUnsortedTopRaid(start, end);
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopRaidResult(start, end, top, TopRaidResult.Type.WEEK);
    }

    public TopRaidResult getTopRaidWeekGroup(GroupId groupId) {
        final var start = TimeUtils.thisWeekMonday();
        final var end = TimeUtils.thisWeekSunday();
        final var top = topDao.getUnsortedTopRaidGroup(start, end, groupId);
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopRaidResult(start, end, top, TopRaidResult.Type.WEEK_GROUP);
    }

    public TopSpinResult getTopSpinGroup(GroupId groupId) {
        final var top = topDao.getUnsortedTopSpinGroup(groupId);
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopSpinResult(top, TopSpinResult.Type.GROUP);
    }

    public GroupTopRaidResult getGroupRaidWeek() {
        final var start = TimeUtils.thisWeekMonday();
        final var end = TimeUtils.thisWeekSunday();
        final var top = topDao.getUnsortedGroupTopRaid(start, end);
        top.sort(Comparator.comparingInt(GroupTopRaidPosition::score).reversed());
        return new GroupTopRaidResult(start, end, top);
    }
}
