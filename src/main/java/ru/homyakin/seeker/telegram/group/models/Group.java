package ru.homyakin.seeker.telegram.group.models;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.event.models.ZeroEnabledEventIntervalsError;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.database.GroupDao;

public record Group(
    GroupId id,
    boolean isActive,
    Language language,
    GroupSettings settings
) {
    // TODO убрать dao из класса
    public Group activate(GroupDao groupDao) {
        return changeActive(true, groupDao);
    }

    public Group deactivate(GroupDao groupDao) {
        return changeActive(false, groupDao);
    }

    public Group changeLanguage(Language newLanguage, GroupDao groupDao) {
        if (language != newLanguage) {
            final var group = new Group(
                id,
                isActive,
                newLanguage,
                settings
            );
            groupDao.update(group);
            return group;
        }
        return this;
    }

    public Either<ZeroEnabledEventIntervalsError, Group> toggleEventInterval(int intervalIndex) {
        return settings.toggleEventInterval(intervalIndex).map(this::copyWithSettings);
    }

    public Either<IncorrectTimeZone, Group> changeTimeZone(int timeZone) {
        return settings.changeTimeZone(timeZone).map(this::copyWithSettings);
    }

    private Group changeActive(boolean newActive, GroupDao groupDao) {
        if (isActive != newActive) {
            final var group = new Group(
                id,
                newActive,
                language,
                settings
            );
            groupDao.update(group);
            return group;
        }
        return this;
    }

    private Group copyWithSettings(GroupSettings settings) {
        return new Group(id, isActive, language, settings);
    }
}
