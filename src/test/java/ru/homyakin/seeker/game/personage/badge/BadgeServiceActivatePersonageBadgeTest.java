package ru.homyakin.seeker.game.personage.badge;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.TestRandom;

public class BadgeServiceActivatePersonageBadgeTest {
    private final BadgeDao badgeDao = Mockito.mock(BadgeDao.class);
    private final BadgeService service = new BadgeService(badgeDao);

    @Test
    public void Given_ActivatingDifferentAvailableBadge_When_ActivateBadge_Then_Success() {
        final var personageId = randomPersonageId();
        final var activeAvailableBadge = new PersonageAvailableBadge(
            personageId,
            new Badge(
                TestRandom.nextInt(),
                BadgeView.FIRST_PERSONAGES,
                Collections.emptyMap()
            ),
            true
        );
        final var disabledAvailableBadge = new PersonageAvailableBadge(
            personageId,
            new Badge(
                TestRandom.nextInt(),
                BadgeView.STANDARD,
                Collections.emptyMap()
            ),
            false
        );
        Mockito.when(badgeDao.getPersonageAvailableBadges(personageId))
            .thenReturn(List.of(activeAvailableBadge, disabledAvailableBadge));

        final var result = service.activatePersonageBadge(personageId, disabledAvailableBadge.badge());

        Mockito.verify(badgeDao, Mockito.times(1))
            .activatePersonageBadge(personageId, disabledAvailableBadge.badge());
        Assertions.assertTrue(result.isRight());
    }

    @Test
    public void Given_ActivatingActiveBadge_When_ActivateBadge_Then_ReturnAlreadyActive() {
        final var personageId = randomPersonageId();
        final var activeAvailableBadge = new PersonageAvailableBadge(
            personageId,
            new Badge(
                TestRandom.nextInt(),
                BadgeView.FIRST_PERSONAGES,
                Collections.emptyMap()
            ),
            true
        );
        Mockito.when(badgeDao.getPersonageAvailableBadges(personageId))
            .thenReturn(List.of(activeAvailableBadge));

        final var result = service.activatePersonageBadge(personageId, activeAvailableBadge.badge());

        Assertions.assertEquals(ActivatePersonageBadgeError.AlreadyActivated.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_ActivatingNotAvailableBadge_When_ActivateBadge_Then_ReturnBadgeIsNotAvailable() {
        final var personageId = randomPersonageId();
        final var activeAvailableBadge = new PersonageAvailableBadge(
            personageId,
            new Badge(
                TestRandom.nextInt(),
                BadgeView.FIRST_PERSONAGES,
                Collections.emptyMap()
            ),
            true
        );
        Mockito.when(badgeDao.getPersonageAvailableBadges(personageId))
            .thenReturn(List.of(activeAvailableBadge));

        final var result = service.activatePersonageBadge(
            personageId, new Badge(TestRandom.nextInt(), BadgeView.STANDARD, Collections.emptyMap())
        );

        Assertions.assertEquals(ActivatePersonageBadgeError.BadgeIsNotAvailable.INSTANCE, result.getLeft());
    }

    private PersonageId randomPersonageId() {
        return PersonageId.from(TestRandom.nextLong());
    }
}
