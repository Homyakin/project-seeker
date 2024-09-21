package ru.homyakin.seeker.telegram.statistic;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.group.GroupService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatisticServiceTest {
    private final GroupService groupService = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final StatisticService statisticService = new StatisticService(groupService, personageService);

    @Test
    public void correctGetStatistic() {
        final var activePersonages = RandomUtils.nextLong();
        final var activeGroups = RandomUtils.nextLong();
        Mockito.when(groupService.getActiveGroupsCount()).thenReturn(activeGroups);
        Mockito.when(personageService.getActivePersonagesCount(Mockito.any())).thenReturn(activePersonages);

        final var statistic = statisticService.getStatistic();

        assertEquals(activePersonages, statistic.activePersonages());
        assertEquals(activeGroups, statistic.activeGroups());
    }
}