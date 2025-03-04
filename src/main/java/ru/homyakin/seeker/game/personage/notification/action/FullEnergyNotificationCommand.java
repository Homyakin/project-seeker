package ru.homyakin.seeker.game.personage.notification.action;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.entity.NotificationError;
import ru.homyakin.seeker.utils.models.Success;

public interface FullEnergyNotificationCommand {

    Either<NotificationError, Success> notifyAboutFullEnergy(PersonageId personageId);
}
