package ru.homyakin.seeker.telegram.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.infrastructure.CachingConfig;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class StatisticService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GroupTgService groupTgService;
    private final PersonageService personageService;

    public StatisticService(GroupTgService groupTgService, PersonageService personageService) {
        this.groupTgService = groupTgService;
        this.personageService = personageService;
    }

    @Cacheable(value = CachingConfig.TELEGRAM_STATISTIC)
    public Statistic getStatistic() {
        logger.debug("Getting statistic");
        final var activePersonages = personageService.getActivePersonagesCount(TimeUtils.moscowTime().minusHours(24));
        final var activeGroups = groupTgService.getActiveGroupsCount();

        return new Statistic(activePersonages, activeGroups);
    }
}
