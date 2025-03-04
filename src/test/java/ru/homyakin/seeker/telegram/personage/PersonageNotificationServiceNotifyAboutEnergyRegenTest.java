package ru.homyakin.seeker.telegram.personage;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.entity.NotificationError;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSetting;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.models.TelegramError;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonageNotificationServiceNotifyAboutEnergyRegenTest {
    private final UserService userService = mock();
    private final GetPersonageSettingsCommand getPersonageSettingsCommand = mock();
    private final TelegramSender telegramSender = mock();
    private final PersonageNotificationService personageNotificationService = new PersonageNotificationService(
        userService,
        getPersonageSettingsCommand,
        telegramSender
    );

    @BeforeAll
    public static void init() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void When_UserIsActiveAndNotificationsAreEnabled_Then_Notify() {
        final var personageId = new PersonageId(1);
        final var user = user(true, personageId);
        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(
            new PersonageSettings(Map.of(PersonageSetting.SEND_NOTIFICATIONS, true))
        );
        when(telegramSender.send(any(SendMessage.class))).thenReturn(Either.right(mock(Message.class)));

        final var result = personageNotificationService.notifyAboutFullEnergy(personageId);

        Assertions.assertTrue(result.isRight());
        verify(telegramSender).send(any(SendMessage.class));
    }

    @Test
    void When_UserIsActiveAndNotificationsAreDisabled_Then_NotNotifyAndReturnSuccess() {
        final var personageId = new PersonageId(1);
        final var user = user(true, personageId);
        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(
            new PersonageSettings(Map.of(PersonageSetting.SEND_NOTIFICATIONS, false))
        );

        final var result = personageNotificationService.notifyAboutFullEnergy(personageId);

        Assertions.assertTrue(result.isRight());
        verify(telegramSender, never()).send(any(SendMessage.class));
    }

    @Test
    void When_UserIsActiveAndNotificationsAreEnabled_Then_NotNotifyAndReturnSuccess() {
        PersonageId personageId = new PersonageId(1);
        User user = user(false, personageId);
        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(
            new PersonageSettings(Map.of(PersonageSetting.SEND_NOTIFICATIONS, true))
        );

        Either<NotificationError, Success> result = personageNotificationService.notifyAboutFullEnergy(personageId);

        Assertions.assertTrue(result.isRight());
        verify(telegramSender, never()).send(any(SendMessage.class));
    }

    @Test
    void When_TelegramSendFails_Then_ReturnFail() {
        PersonageId personageId = new PersonageId(1);
        User user = user(true, personageId);
        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(
            new PersonageSettings(Map.of(PersonageSetting.SEND_NOTIFICATIONS, true))
        );
        when(telegramSender.send(any(SendMessage.class))).thenReturn(Either.left(mock(TelegramError.InternalError.class)));

        final var result = personageNotificationService.notifyAboutFullEnergy(personageId);

        Assertions.assertTrue(result.isLeft());
        verify(telegramSender).send(any(SendMessage.class));
    }

    private User user(boolean isActivePrivateMessages, PersonageId personageId) {
        return new User(
            new UserId(1),
            isActivePrivateMessages,
            Language.EN,
            personageId,
            Optional.empty()
        );
    }
}
