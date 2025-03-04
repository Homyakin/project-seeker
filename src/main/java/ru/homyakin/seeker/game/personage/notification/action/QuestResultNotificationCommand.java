package ru.homyakin.seeker.game.personage.notification.action;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.entity.NotificationError;
import ru.homyakin.seeker.utils.models.Success;

public interface QuestResultNotificationCommand {
    Either<NotificationError, Success> notifyAboutQuestResult(
        PersonageId personageId,
        EventResult.PersonalQuestResult result
    );
}
