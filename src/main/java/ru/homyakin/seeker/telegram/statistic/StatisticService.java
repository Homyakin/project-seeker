package ru.homyakin.seeker.telegram.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.group.action.CountActiveRegisteredGroupsCommand;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.infrastructure.CachingConfig;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class StatisticService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CountActiveRegisteredGroupsCommand countActiveRegisteredGroupsCommand;
    private final PersonageService personageService;

    public StatisticService(CountActiveRegisteredGroupsCommand countActiveRegisteredGroupsCommand, PersonageService personageService) {
        this.countActiveRegisteredGroupsCommand = countActiveRegisteredGroupsCommand;
        this.personageService = personageService;
    }

    @Cacheable(value = CachingConfig.TELEGRAM_STATISTIC)
    public Statistic getStatistic() {
        logger.debug("Getting statistic");
        final var activePersonages = personageService.getActivePersonagesCount(TimeUtils.moscowTime().minusHours(24));
        final var activeRegisteredGroups = countActiveRegisteredGroupsCommand.execute();

        return new Statistic(activePersonages, activeRegisteredGroups);
    }
}
