package ru.homyakin.seeker.game.badge.action;

import io.vavr.control.Either;
import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.game.badge.entity.ActivateBadgeError;
import ru.homyakin.seeker.game.badge.entity.AvailableBadge;
import ru.homyakin.seeker.utils.models.Success;

import java.util.List;
import java.util.Objects;

public abstract class ManipulateBadgeService<T> {

    public abstract List<AvailableBadge> getAvailableBadges(T id);

    public final Either<ActivateBadgeError, Success> activateBadge(T id, BadgeId badgeId) {
        return getAvailableBadges(id).stream()
            .filter(availableBadge -> Objects.equals(availableBadge.badge().id(), badgeId))
            .findFirst()
            .<Either<ActivateBadgeError, Success>>map(availableBadge -> {
                if (availableBadge.isActive()) {
                    return Either.left(ActivateBadgeError.AlreadyActivated.INSTANCE);
                } else {
                    activateBadgeInStorage(id, availableBadge.badge().id());
                    return Either.right(Success.INSTANCE);
                }
            })
            .orElseGet(() -> Either.left(ActivateBadgeError.BadgeIsNotAvailable.INSTANCE));
    }

    protected abstract void activateBadgeInStorage(T id, BadgeId badgeId);
}
