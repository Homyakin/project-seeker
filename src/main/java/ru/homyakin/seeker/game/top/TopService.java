package ru.homyakin.seeker.game.top;

import java.util.Comparator;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.top.models.PersonageTopPosition;
import ru.homyakin.seeker.game.top.models.PersonageTopResult;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.game.top.models.TopSpinPosition;
import ru.homyakin.seeker.game.top.models.TopSpinResult;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class TopService {
    private final TopDao topDao;

    public TopService(TopDao topDao) {
        this.topDao = topDao;
    }

    public PersonageTopResult<TopRaidPosition> getTopRaidWeek() {
        final var start = TimeUtils.thisWeekMonday();
        final var end = TimeUtils.thisWeekSunday();
        final var top = topDao.getUnsortedTopRaid(start, end);
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopRaidResult(start, end, top, TopRaidResult.Type.WEEK);
    }

    public PersonageTopResult<TopRaidPosition> getTopRaidWeekGroup(GroupTgId groupId) {
        final var start = TimeUtils.thisWeekMonday();
        final var end = TimeUtils.thisWeekSunday();
        final var top = topDao.getUnsortedTopRaidGroup(start, end, groupId);
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopRaidResult(start, end, top, TopRaidResult.Type.WEEK_GROUP);
    }

    public PersonageTopResult<TopSpinPosition> getTopSpinGroup(GroupTgId groupId) {
        final var top = topDao.getUnsortedTopSpinGroup(groupId);
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopSpinResult(top, TopSpinResult.Type.GROUP);
    }
}
