package ru.homyakin.seeker.game.personage;


import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.action.FullEnergyNotificationCommand;
import ru.homyakin.seeker.game.personage.notification.entity.NotificationError;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegenEnergySchedulerTest {
    private final PersonageService personageService = mock();
    private final PersonageDao personageDao = mock();
    private final FullEnergyNotificationCommand fullEnergyNotificationCommand = mock();
    private final RegenEnergyScheduler regenEnergyScheduler = new RegenEnergyScheduler(
        personageService,
        personageDao,
        fullEnergyNotificationCommand
    );

    @Test
    void When_MultiplePersonagesHaveRecoveredEnergy_Then_NotifyUsersAndRegenerate() {
        PersonageId personageId1 = new PersonageId(1);
        PersonageId personageId2 = new PersonageId(2);
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(Arrays.asList(personageId1, personageId2));
        when(fullEnergyNotificationCommand.notifyAboutFullEnergy(any())).thenReturn(Either.right(Success.INSTANCE));

        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        verify(fullEnergyNotificationCommand, times(2)).notifyAboutFullEnergy(any());
        verify(personageService, times(2)).getByIdForce(any());
    }

    @Test
    void When_NoPersonagesHaveRecoveredEnergy_Then_NoNotificationSent() {
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(Collections.emptyList());

        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        verify(fullEnergyNotificationCommand, never()).notifyAboutFullEnergy(any());
    }

    @Test
    void When_NotificationServiceIsUnavailable_Then_DontRegenerate() {
        PersonageId personageId = new PersonageId(1);
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(Collections.singletonList(personageId));
        when(fullEnergyNotificationCommand.notifyAboutFullEnergy(any())).thenReturn(Either.left(NotificationError.INSTANCE));

        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        verify(fullEnergyNotificationCommand).notifyAboutFullEnergy(personageId);
        verify(personageService, never()).getByIdForce(any());
    }


}
