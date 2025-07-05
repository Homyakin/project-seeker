package ru.homyakin.seeker.game.top;

import java.util.Comparator;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.group.action.personage.GetActiveGroupPersonagesCommand;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.season.action.SeasonService;
import ru.homyakin.seeker.game.top.models.GroupTopRaidPosition;
import ru.homyakin.seeker.game.top.models.GroupTopRaidResult;
import ru.homyakin.seeker.game.top.models.PersonageTopPosition;
import ru.homyakin.seeker.game.top.models.TopDonatePosition;
import ru.homyakin.seeker.game.top.models.TopDonateResult;
import ru.homyakin.seeker.game.top.models.TopPowerPersonagePosition;
import ru.homyakin.seeker.game.top.models.TopPowerPersonageResult;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.game.top.models.TopWorkerOfDayResult;
import ru.homyakin.seeker.game.top.models.TopWorldRaidResearchResult;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class TopService {
    private final TopDao topDao;
    private final GetActiveGroupPersonagesCommand getActiveGroupPersonagesCommand;
    private final PersonageService personageService;
    private final SeasonService seasonService;

    public TopService(
        TopDao topDao,
        GetActiveGroupPersonagesCommand getActiveGroupPersonagesCommand,
        PersonageService personageService,
        SeasonService seasonService
    ) {
        this.topDao = topDao;
        this.getActiveGroupPersonagesCommand = getActiveGroupPersonagesCommand;
        this.personageService = personageService;
        this.seasonService = seasonService;
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

    public TopWorkerOfDayResult getTopWorkerOfDayGroup(GroupId groupId) {
        final var top = topDao.getUnsortedTopWorkerGroup(groupId);
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopWorkerOfDayResult(top, TopWorkerOfDayResult.Type.GROUP);
    }

    public TopDonateResult getTopDonateGroup(GroupId groupId) {
        final var currentSeason = seasonService.currentSeason();
        final var top = topDao.getUnsortedTopDonateGroup(groupId, currentSeason.value());
        top.sort(Comparator.comparingLong(TopDonatePosition::donateMoney).reversed());
        return new TopDonateResult(top, currentSeason);
    }

    public GroupTopRaidResult getGroupRaidWeek() {
        final var start = TimeUtils.thisWeekMonday();
        final var end = TimeUtils.thisWeekSunday();
        final var top = topDao.getUnsortedGroupTopRaid(start, end);
        top.sort(Comparator.comparingInt(GroupTopRaidPosition::score).reversed());
        return new GroupTopRaidResult(start, end, top);
    }

    public TopPowerPersonageResult getTopPowerPersonage(GroupId groupId) {
        final var personages = personageService.getByIds(getActiveGroupPersonagesCommand.execute(groupId));
        final var top = personages.stream()
            .map(Personage::toBattlePersonage)
            .sorted(Comparator.comparingDouble(BattlePersonage::power).reversed())
            .map(it -> new TopPowerPersonagePosition(
                it.personage().id(),
                it.personage().name(),
                it.personage().badge(),
                it.personage().tag(),
                (int) it.power()
            ))
            .toList();
        return new TopPowerPersonageResult(top);
    }

    public TopWorldRaidResearchResult getTopWorldRaidResearch() {
        final var top = topDao.getUnsortedTopWorldRaidResearch();
        top.sort(Comparator.comparingInt(PersonageTopPosition::score).reversed());
        return new TopWorldRaidResearchResult(top);
    }
}
