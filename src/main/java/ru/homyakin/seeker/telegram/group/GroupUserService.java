package ru.homyakin.seeker.telegram.group;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.utils.models.Pair;
import ru.homyakin.seeker.telegram.group.database.GroupUserDao;
import ru.homyakin.seeker.telegram.group.models.GroupUser;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;

@Service
public class GroupUserService {
    private final GroupService groupService;
    private final UserService userService;
    private final GroupUserDao groupUserDao;

    public GroupUserService(GroupService groupService, UserService userService, GroupUserDao groupUserDao) {
        this.groupService = groupService;
        this.userService = userService;
        this.groupUserDao = groupUserDao;
    }

    public Pair<Group, User> getAndActivateOrCreate(long groupId, long userId) {
        final var group = groupService.getOrCreate(groupId);
        final var user = userService.getOrCreateFromGroup(userId);
        groupUserDao.getByGroupIdAndUserId(groupId, userId)
            .ifPresentOrElse(
                groupUser -> groupUser.activate(groupUserDao),
                () -> groupUserDao.save(new GroupUser(groupId, userId, true))
            );
        return new Pair<>(group, user);
    }
}
