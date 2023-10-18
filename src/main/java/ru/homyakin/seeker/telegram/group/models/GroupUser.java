package ru.homyakin.seeker.telegram.group.models;

import ru.homyakin.seeker.telegram.group.database.GroupUserDao;
import ru.homyakin.seeker.telegram.user.models.UserId;

//TODO добавить обработку выхода из чата
public record GroupUser(
    long groupId,
    UserId userId,
    boolean isActive
) {
    public GroupUser activate(GroupUserDao groupUserDao) {
        return changeActive(true, groupUserDao);
    }

    public GroupUser deactivate(GroupUserDao groupUserDao) {
        return changeActive(false, groupUserDao);
    }

    private GroupUser changeActive(boolean newActive, GroupUserDao groupUserDao) {
        if (isActive != newActive) {
            final var groupUser = new GroupUser(
                groupId,
                userId,
                newActive
            );
            groupUserDao.update(groupUser);
            return groupUser;
        }
        return this;
    }
}
