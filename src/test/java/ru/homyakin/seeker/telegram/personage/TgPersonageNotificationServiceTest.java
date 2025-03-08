package ru.homyakin.seeker.telegram.personage;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TgPersonageNotificationServiceTest {
    private final UserService userService = mock();
    private final TelegramSender telegramSender = mock();
    private final GetPersonageSettingsCommand getPersonageSettingsCommand = mock();
    private final TgPersonageNotificationService notificationService = new TgPersonageNotificationService(
        userService,
        getPersonageSettingsCommand,
        telegramSender
    );

    @BeforeAll
    public static void init() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void When_UserHasPrivateMessagesAndEnabledNotifications_Then_NotificationSent() {
        // Given
        PersonageId personageId = new PersonageId(1);
        User user = createUser(personageId, true);
        Notification notification = Notification.RecoveredEnergy.INSTANCE;

        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);
        when(telegramSender.send(any(SendMessage.class))).thenReturn(Either.right(new Message()));
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(createPersonageSettings(true));

        // When
        Either<NotificationError, Success> result = notificationService.sendNotification(personageId, notification);

        // Then
        Assertions.assertTrue(result.isRight());
        verify(telegramSender, times(1)).send(any(SendMessage.class));
    }

    @Test
    void When_UserHasDisabledPrivateMessages_Then_NotificationNotSentAndReturnSuccess() {
        // Given
        PersonageId personageId = new PersonageId(2);
        User user = createUser(personageId, false);
        Notification notification = Notification.RecoveredEnergy.INSTANCE;

        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);

        // When
        Either<NotificationError, Success> result = notificationService.sendNotification(personageId, notification);

        // Then
        Assertions.assertTrue(result.isRight());
        verify(telegramSender, never()).send(any(SendMessage.class));
    }

    @Test
    void When_PersonageHasDisabledNotifications_Then_NotificationNotSentAndReturnSuccess() {
        // Given
        PersonageId personageId = new PersonageId(2);
        User user = createUser(personageId, true);
        Notification notification = Notification.RecoveredEnergy.INSTANCE;

        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(createPersonageSettings(false));

        // When
        Either<NotificationError, Success> result = notificationService.sendNotification(personageId, notification);

        // Then
        Assertions.assertTrue(result.isRight());
        verify(telegramSender, never()).send(any(SendMessage.class));
    }

    @Test
    void When_TelegramReturnError_Then_NotificationNotSent() {
        // Given
        PersonageId personageId = new PersonageId(3);
        User user = new User(new UserId(3), true, Language.EN, personageId, Optional.empty());
        Notification notification = Notification.RecoveredEnergy.INSTANCE;

        when(userService.getByPersonageIdForce(personageId)).thenReturn(user);
        when(getPersonageSettingsCommand.execute(personageId)).thenReturn(createPersonageSettings(true));
        when(telegramSender.send(any(SendMessage.class)))
            .thenReturn(Either.left(new TelegramError.InternalError("Bot was blocked")));

        // When
        Either<NotificationError, Success> result = notificationService.sendNotification(personageId, notification);

        // Then
        Assertions.assertTrue(result.isLeft());
        verify(telegramSender, times(1)).send(any(SendMessage.class));
    }

    private User createUser(PersonageId personageId, boolean isActivePrivateMessages) {
        return new User(new UserId(1), isActivePrivateMessages, Language.EN, personageId, Optional.empty());
    }

    private PersonageSettings createPersonageSettings(boolean sendNotifications) {
        return new PersonageSettings(Map.of(
            PersonageSetting.SEND_NOTIFICATIONS, sendNotifications
        ));
    }
}
