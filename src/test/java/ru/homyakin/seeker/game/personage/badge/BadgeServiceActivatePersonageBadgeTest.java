package ru.homyakin.seeker.game.personage.badge;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.game.badge.action.PersonageBadgeService;
import ru.homyakin.seeker.game.badge.entity.ActivateBadgeError;
import ru.homyakin.seeker.game.badge.entity.Badge;
import ru.homyakin.seeker.game.badge.entity.PersonageBadgeStorage;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.badge.entity.AvailableBadge;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.TestRandom;

public class BadgeServiceActivatePersonageBadgeTest {
    private final PersonageBadgeStorage storage = Mockito.mock();
    private final PersonageBadgeService service = new PersonageBadgeService(Mockito.mock(), storage);

    @Test
    public void Given_ActivatingDifferentAvailableBadge_When_ActivateBadge_Then_Success() {
        final var personageId = randomPersonageId();
        final var activeAvailableBadge = new AvailableBadge(
            new Badge(
                BadgeId.of(TestRandom.nextInt()),
                BadgeView.FIRST_PERSONAGES,
                Collections.emptyMap()
            ),
            true
        );
        final var disabledAvailableBadge = new AvailableBadge(
            new Badge(
                BadgeId.of(TestRandom.nextInt()),
                BadgeView.STANDARD,
                Collections.emptyMap()
            ),
            false
        );
        Mockito.when(storage.getPersonageAvailableBadges(personageId))
            .thenReturn(List.of(activeAvailableBadge, disabledAvailableBadge));

        final var result = service.activateBadge(personageId, disabledAvailableBadge.badge().id());

        Mockito.verify(storage, Mockito.times(1))
            .activatePersonageBadge(personageId, disabledAvailableBadge.badge().id());
        Assertions.assertTrue(result.isRight());
    }

    @Test
    public void Given_ActivatingActiveBadge_When_ActivateBadge_Then_ReturnAlreadyActive() {
        final var personageId = randomPersonageId();
        final var activeAvailableBadge = new AvailableBadge(
            new Badge(
                BadgeId.of(TestRandom.nextInt()),
                BadgeView.FIRST_PERSONAGES,
                Collections.emptyMap()
            ),
            true
        );
        Mockito.when(storage.getPersonageAvailableBadges(personageId))
            .thenReturn(List.of(activeAvailableBadge));

        final var result = service.activateBadge(personageId, activeAvailableBadge.badge().id());

        Assertions.assertEquals(ActivateBadgeError.AlreadyActivated.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_ActivatingNotAvailableBadge_When_ActivateBadge_Then_ReturnBadgeIsNotAvailable() {
        final var personageId = randomPersonageId();
        final var activeAvailableBadge = new AvailableBadge(
            new Badge(
                BadgeId.of(TestRandom.nextInt()),
                BadgeView.FIRST_PERSONAGES,
                Collections.emptyMap()
            ),
            true
        );
        Mockito.when(storage.getPersonageAvailableBadges(personageId))
            .thenReturn(List.of(activeAvailableBadge));

        final var result = service.activateBadge(personageId, BadgeId.of(TestRandom.nextInt()));

        Assertions.assertEquals(ActivateBadgeError.BadgeIsNotAvailable.INSTANCE, result.getLeft());
    }

    private PersonageId randomPersonageId() {
        return PersonageId.from(TestRandom.nextLong());
    }
}
