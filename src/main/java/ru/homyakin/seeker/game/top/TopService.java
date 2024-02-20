package ru.homyakin.seeker.game.top;

import java.util.Comparator;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.top.models.TopPosition;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.game.top.models.TopResult;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class TopService {
    private final TopDao topDao;

    public TopService(TopDao topDao) {
        this.topDao = topDao;
    }

    public TopResult getTopRaidWeek() {
        final var start = TimeUtils.thisWeekMonday();
        final var end = TimeUtils.thisWeekSunday();
        final var top = topDao.getUnsortedTopRaid(start, end);
        top.sort(Comparator.comparingInt(TopPosition::score).reversed());
        return new TopRaidResult(start, end, top);
    }
}
