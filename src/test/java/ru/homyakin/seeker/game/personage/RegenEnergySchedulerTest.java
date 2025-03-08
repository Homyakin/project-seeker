package ru.homyakin.seeker.game.personage;


import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.personal_quest.PersonalQuestService;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.TakeQuestError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.action.SendNotificationToPersonageCommand;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
import ru.homyakin.seeker.game.personage.notification.entity.NotificationError;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;
import ru.homyakin.seeker.utils.models.Success;

import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegenEnergySchedulerTest {
    private final PersonageService personageService = mock();
    private final PersonageDao personageDao = mock();
    private final GetPersonageSettingsCommand getPersonageSettingsCommand = mock();
    private final PersonalQuestService personalQuestService = mock();
    private final SendNotificationToPersonageCommand sendNotificationToPersonageCommand = mock();
    private final RegenEnergyScheduler regenEnergyScheduler = new RegenEnergyScheduler(
        personageService,
        personageDao,
        getPersonageSettingsCommand,
        personalQuestService,
        sendNotificationToPersonageCommand
    );

    @Test
    void Given_PersonageWithDisabledAutoQuest_When_SuccessNotification_Then_RegenEnergy() {
        // given
        PersonageId personageId = new PersonageId(1);
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(List.of(personageId));
        when(sendNotificationToPersonageCommand.sendNotification(any(), any()))
            .thenReturn(Either.right(Success.INSTANCE));
        mockDisableAutoQuest(personageId);

        // when
        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        // then
        verify(sendNotificationToPersonageCommand)
            .sendNotification(eq(personageId), any(Notification.RecoveredEnergy.class));
        verify(personageService).getByIdForce(personageId);
    }

    @Test
    void Given_PersonageWithDisabledAutoQuest_When_NotSuccessNotification_Then_NotRegenEnergy() {
        // given
        PersonageId personageId = new PersonageId(1);
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(List.of(personageId));
        when(sendNotificationToPersonageCommand.sendNotification(any(), any(Notification.RecoveredEnergy.class)))
            .thenReturn(Either.left(NotificationError.INSTANCE));
        mockDisableAutoQuest(personageId);

        // when
        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        // then
        verify(sendNotificationToPersonageCommand)
            .sendNotification(eq(personageId), any(Notification.RecoveredEnergy.class));
        verify(personageService, times(0)).getByIdForce(personageId);
    }

    @Test
    void Given_PersonageAutoQuestingEnabled_When_SuccessStartQuest_Then_SendNotification() {
        // given
        PersonageId personageId = new PersonageId(1);
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(List.of(personageId));
        mockEnabledAutoQuest(personageId);
        when(personalQuestService.autoStartQuest(personageId)).thenReturn(Either.right(mock(StartedQuest.class)));

        // when
        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        // then
        verify(personalQuestService).autoStartQuest(personageId);
        verify(sendNotificationToPersonageCommand)
            .sendNotification(eq(personageId), any(Notification.AutoStartQuest.class));
    }

    @Test
    void Given_PersonageAutoQuestingEnabled_When_FailureStartQuest_Then_NotSendNotification() {
        // given
        PersonageId personageId = new PersonageId(1);
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(List.of(personageId));
        mockEnabledAutoQuest(personageId);
        when(personalQuestService.autoStartQuest(personageId))
            .thenReturn(Either.left(TakeQuestError.PersonageLocked.INSTANCE));

        // when
        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        // then
        verify(personalQuestService).autoStartQuest(personageId);
        verify(sendNotificationToPersonageCommand, times(0))
            .sendNotification(eq(personageId), any(Notification.AutoStartQuest.class));
    }

    @Test
    void Given_TwoPersonageWithDifferentAutoQuest_Then_DoAutoQuestAndRecoverEnergy() {
        // given
        PersonageId personageId1 = new PersonageId(1);
        PersonageId personageId2 = new PersonageId(2);
        when(personageDao.getPersonagesWithRecoveredEnergy()).thenReturn(List.of(personageId1, personageId2));
        when(sendNotificationToPersonageCommand.sendNotification(any(), any()))
            .thenReturn(Either.right(Success.INSTANCE));

        mockEnabledAutoQuest(personageId1);
        when(personalQuestService.autoStartQuest(personageId1)).thenReturn(Either.right(mock(StartedQuest.class)));

        mockDisableAutoQuest(personageId2);

        // when
        regenEnergyScheduler.notifyUsersAboutEnergyRegen();

        // then
        verify(personalQuestService).autoStartQuest(personageId1);
        verify(sendNotificationToPersonageCommand)
            .sendNotification(eq(personageId1), any(Notification.AutoStartQuest.class));

        verify(personageService).getByIdForce(personageId2);
        verify(sendNotificationToPersonageCommand)
            .sendNotification(eq(personageId2), any(Notification.RecoveredEnergy.class));
    }

    private void mockDisableAutoQuest(PersonageId personageId) {
        PersonageSettings settings = mock(PersonageSettings.class);
        when(settings.autoQuesting()).thenReturn(false);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(settings);
    }

    private void mockEnabledAutoQuest(PersonageId personageId) {
        PersonageSettings settings = mock(PersonageSettings.class);
        when(settings.autoQuesting()).thenReturn(true);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(settings);
    }
}
