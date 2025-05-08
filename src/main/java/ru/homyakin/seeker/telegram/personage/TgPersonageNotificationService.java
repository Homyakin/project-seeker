package ru.homyakin.seeker.telegram.personage;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.action.SendNotificationToPersonageCommand;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
import ru.homyakin.seeker.game.personage.notification.entity.NotificationError;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.BulletinBoardLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.models.Success;

@Service
public class TgPersonageNotificationService implements SendNotificationToPersonageCommand {
    private static final Logger logger = LoggerFactory.getLogger(TgPersonageNotificationService.class);
    private final UserService userService;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;
    private final TelegramSender telegramSender;

    public TgPersonageNotificationService(
        UserService userService,
        GetPersonageSettingsCommand getPersonageSettingsCommand,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public Either<NotificationError, Success> sendNotification(PersonageId personageId, Notification notification) {
        final var user = userService.getByPersonageIdForce(personageId);
        logger.info(
            "Sending notification {} to user {}",
            notification.getClass().getSimpleName(),
            user.id()
        );
        if (!needToNotify(user)) {
            return Either.right(Success.INSTANCE);
        }
        return telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(user.id())
                    .text(getText(notification, user.language()))
                    .build()
            )
            .mapLeft(_ -> NotificationError.INSTANCE)
            .map(_ -> Success.INSTANCE);
    }

    private boolean needToNotify(User user) {
        if (!user.isActivePrivateMessages()) {
            logger.info("Skip notify user {}, disabled in private", user.id());
            return false;
        }
        final var settings = getPersonageSettingsCommand.execute(user.personageId());
        if (!settings.sendNotifications()) {
            logger.info("Skip notify user {}, disabled in settings", user.id());
            return false;
        }
        return true;
    }

    public String getText(Notification notification, Language language) {
        return switch (notification) {
            case Notification.RecoveredEnergy _ ->
                CommonLocalization.energyRecovered(language);
            case Notification.QuestResult questResult ->
                BulletinBoardLocalization.personalQuestResult(language, questResult.value());
            case Notification.AutoStartQuest autoStartQuest ->
                BulletinBoardLocalization.autoStartedQuest(language, autoStartQuest.startedQuest());
        };
    }
}
