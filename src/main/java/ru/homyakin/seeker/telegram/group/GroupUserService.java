package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.models.Pair;
import ru.homyakin.seeker.telegram.group.database.GroupUserDao;
import ru.homyakin.seeker.telegram.group.models.GroupUser;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.utils.models.errors.TelegramError;

@Service
public class GroupUserService {
    private final GroupService groupService;
    private final UserService userService;
    private final GroupUserDao groupUserDao;
    private final TelegramSender telegramSender;

    public GroupUserService(GroupService groupService, UserService userService, GroupUserDao groupUserDao, TelegramSender telegramSender) {
        this.groupService = groupService;
        this.userService = userService;
        this.groupUserDao = groupUserDao;
        this.telegramSender = telegramSender;
    }

    public Either<TelegramError, Boolean> isUserAdminInGroup(Long groupId, Long userId) {
        return telegramSender.send(TelegramMethods.createGetChatMember(groupId, userId))
            .map(it -> it instanceof ChatMemberAdministrator || it instanceof ChatMemberOwner);
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

    public Optional<GroupUser> getRandomUserFromGroup(long groupId) {
        return groupUserDao.getRandomUserByGroup(groupId);
    }

    public int countUsersInGroup(long groupId) {
        return groupUserDao.countUsersInGroup(groupId);
    }
}
