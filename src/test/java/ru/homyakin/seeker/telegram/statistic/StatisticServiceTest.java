package ru.homyakin.seeker.telegram.statistic;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.test_utils.TestRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatisticServiceTest {
    private final GroupTgService groupTgService = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final StatisticService statisticService = new StatisticService(groupTgService, personageService);

    @Test
    public void correctGetStatistic() {
        final var activePersonages = TestRandom.nextLong();
        final var activeGroups = TestRandom.nextLong();
        Mockito.when(groupTgService.getActiveGroupsCount()).thenReturn(activeGroups);
        Mockito.when(personageService.getActivePersonagesCount(Mockito.any())).thenReturn(activePersonages);

        final var statistic = statisticService.getStatistic();

        assertEquals(activePersonages, statistic.activePersonages());
        assertEquals(activeGroups, statistic.activeGroups());
    }
}
