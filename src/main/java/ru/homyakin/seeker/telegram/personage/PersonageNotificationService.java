package ru.homyakin.seeker.telegram.personage;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.action.FullEnergyNotificationCommand;
import ru.homyakin.seeker.game.personage.notification.action.QuestResultNotificationCommand;
import ru.homyakin.seeker.game.personage.notification.entity.NotificationError;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.PersonalQuestLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.models.Success;

@Service
public class PersonageNotificationService implements FullEnergyNotificationCommand, QuestResultNotificationCommand {
    private static final Logger logger = LoggerFactory.getLogger(PersonageNotificationService.class);
    private final UserService userService;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;
    private final TelegramSender telegramSender;

    public PersonageNotificationService(
        UserService userService,
        GetPersonageSettingsCommand getPersonageSettingsCommand,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public Either<NotificationError, Success> notifyAboutFullEnergy(PersonageId personageId) {
        final var user = userService.getByPersonageIdForce(personageId);
        if (!needToNotify(personageId, user)) {
            return Either.right(Success.INSTANCE);
        }
        logger.info("Notify user {} about energy regen", user.id());
        return telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(user.id())
                .text(CommonLocalization.energyRecovered(user.language()))
                .build()
        )
            .mapLeft(_ -> NotificationError.INSTANCE)
            .map(_ -> Success.INSTANCE);
    }

    @Override
    public Either<NotificationError, Success> notifyAboutQuestResult(PersonageId personageId, EventResult.PersonalQuestResult result) {
        final var user = userService.getByPersonageIdForce(personageId);
        if (!needToNotify(personageId, user)) {
            return Either.right(Success.INSTANCE);
        }
        final var message = SendMessageBuilder.builder().chatId(user.id());
        switch (result) {
            case EventResult.PersonalQuestResult.Error _ -> {
                return Either.right(Success.INSTANCE);
            }
            case EventResult.PersonalQuestResult.Failure failure ->
                message.text(PersonalQuestLocalization.failedQuest(user.language(), failure));
            case EventResult.PersonalQuestResult.Success success ->
                message.text(PersonalQuestLocalization.successQuest(user.language(), success));
        }
        return telegramSender.send(message.build())
            .mapLeft(_ -> NotificationError.INSTANCE)
            .map(_ -> Success.INSTANCE);
    }

    private boolean needToNotify(PersonageId personageId, User user) {
        if (!user.isActivePrivateMessages()) {
            logger.info("Skip notify user {} about energy regen, disabled in private", user.id());
            return false;
        }
        if (!getPersonageSettingsCommand.execute(personageId).sendNotifications()) {
            logger.info("Notification disabled for personage {}", personageId);
            return false;
        }
        return true;
    }
}
