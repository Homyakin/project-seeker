package ru.homyakin.seeker.game.group.entity;

import io.vavr.control.Either;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.error.StillSame;
import ru.homyakin.seeker.game.group.error.ZeroEnabledEventIntervalsError;
import ru.homyakin.seeker.game.group.error.IncorrectTimeZone;
import ru.homyakin.seeker.game.badge.entity.BadgeView;

import java.util.Optional;

public record Group(
    GroupId id,
    Optional<String> tag,
    String name,
    BadgeView badge,
    boolean isActive,
    GroupSettings settings
) {
    public Either<ZeroEnabledEventIntervalsError, Group> toggleEventInterval(int intervalIndex) {
        return settings.toggleEventInterval(intervalIndex).map(this::copyWithSettings);
    }

    public Either<IncorrectTimeZone, Group> changeTimeZone(int timeZone) {
        return settings.changeTimeZone(timeZone).map(this::copyWithSettings);
    }

    public Either<StillSame, Group> activate() {
        return changeActive(true);
    }

    public Either<StillSame, Group> deactivate() {
        return changeActive(false);
    }

    public boolean isRegistered() {
        return tag.isPresent();
    }

    public boolean isHidden() {
        return settings.isHidden();
    }

    public boolean isSameTag(String tag) {
        return this.tag.map(tag::equals).orElse(false);
    }

    private Group copyWithSettings(GroupSettings settings) {
        return new Group(id, tag, name, badge, isActive, settings);
    }

    private Either<StillSame, Group> changeActive(boolean newActive) {
        if (isActive != newActive) {
            final var group = new Group(
                id,
                tag,
                name,
                badge,
                newActive,
                settings
            );
            return Either.right(group);
        }
        return Either.left(StillSame.INSTANCE);
    }
}
