package ru.homyakin.seeker.telegram.group.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.database.GroupDao;

public record Group(
    long id,
    boolean isActive,
    Language language,
    ActiveTime activeTime
) {
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
                activeTime
            );
            groupDao.update(group);
            return group;
        }
        return this;
    }

    public Group withActiveTime(ActiveTime activeTime) {
        return new Group(
            id,
            isActive,
            language,
            activeTime
        );
    }

    private Group changeActive(boolean newActive, GroupDao groupDao) {
        if (isActive != newActive) {
            final var group = new Group(
                id,
                newActive,
                language,
                activeTime
            );
            groupDao.update(group);
            return group;
        }
        return this;
    }
}
