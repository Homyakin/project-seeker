package ru.homyakin.seeker.telegram.group.models;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.database.GroupDao;

public record GroupTg(
    GroupTgId id,
    Language language,
    GroupId groupId
) {
    // TODO убрать dao из класса
    public GroupTg changeLanguage(Language newLanguage, GroupDao groupDao) {
        if (language != newLanguage) {
            final var group = new GroupTg(
                id,
                newLanguage,
                groupId
            );
            groupDao.update(group);
            return group;
        }
        return this;
    }
}
