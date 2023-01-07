package ru.homyakin.seeker.telegram.group.models;

import java.time.LocalDateTime;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.database.GroupDao;

// TODO добавить дату добавления
public record Group(
    long id,
    boolean isActive,
    Language language,
    LocalDateTime nextEventDate
) {
    public Group activate(GroupDao groupDao) {
        return changeActive(true, groupDao);
    }

    public Group deactivate(GroupDao groupDao) {
        return changeActive(false, groupDao);
    }

    public Group updateNextEventDate(LocalDateTime newNextEventDate, GroupDao groupDao) {
        final var group = new Group(
            id,
            isActive,
            language,
            newNextEventDate
        );
        groupDao.update(group);
        return group;
    }

    public Group changeLanguage(Language newLanguage, GroupDao groupDao) {
        if (language != newLanguage) {
            final var group = new Group(
                id,
                isActive,
                newLanguage,
                nextEventDate
            );
            groupDao.update(group);
            return group;
        }
        return this;
    }

    private Group changeActive(boolean newActive, GroupDao groupDao) {
        if (isActive != newActive) {
            final var group = new Group(
                id,
                newActive,
                language,
                nextEventDate
            );
            groupDao.update(group);
            return group;
        }
        return this;
    }
}
