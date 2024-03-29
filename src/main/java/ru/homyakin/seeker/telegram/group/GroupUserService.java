package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.stats.GroupPersonageStatsService;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.models.Pair;
import ru.homyakin.seeker.telegram.group.database.GroupUserDao;
import ru.homyakin.seeker.telegram.group.models.GroupUser;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.models.TelegramError;

@Service
public class GroupUserService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GroupService groupService;
    private final UserService userService;
    private final GroupUserDao groupUserDao;
    private final TelegramSender telegramSender;
    private final GroupPersonageStatsService groupPersonageStatsService;

    public GroupUserService(
        GroupService groupService,
        UserService userService,
        GroupUserDao groupUserDao,
        TelegramSender telegramSender,
        GroupPersonageStatsService groupPersonageStatsService
    ) {
        this.groupService = groupService;
        this.userService = userService;
        this.groupUserDao = groupUserDao;
        this.telegramSender = telegramSender;
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    public Either<TelegramError, Boolean> isUserAdminInGroup(GroupId groupId, UserId userId) {
        return telegramSender.send(TelegramMethods.createGetChatMember(groupId, userId))
            .map(it -> it instanceof ChatMemberAdministrator || it instanceof ChatMemberOwner);
    }

    public Pair<Group, User> getAndActivateOrCreate(GroupId groupId, UserId userId) {
        final var group = groupService.getOrCreate(groupId);
        final var user = userService.getOrCreateFromGroup(userId);
        groupUserDao.getByGroupIdAndUserId(groupId, userId)
            .ifPresentOrElse(
                groupUser -> groupUser.activate(groupUserDao),
                () -> createGroupUser(group, user)
            );
        return Pair.of(group, user);
    }

    public Optional<User> getRandomUserFromGroup(GroupId groupId) {
        return groupUserDao.getRandomUserByGroup(groupId)
            .map(groupUser -> userService.getOrCreateFromGroup(groupUser.userId()));
    }

    public Either<TelegramError.InternalError, Boolean> isUserStillInGroup(GroupId groupId, UserId userId) {
        final var result =  telegramSender.send(TelegramMethods.createGetChatMember(groupId, userId));
        if (result.isLeft()) {
            return switch (result.getLeft()) {
                case TelegramError.UserNotFound _ -> {
                    logger.warn("User {} is no longer in group {}", userId.value(), groupId.value());
                    deactivateGroupUser(groupId, userId);
                    yield Either.right(false);
                }
                case TelegramError.InternalError internalError -> Either.left(internalError);
            };
        }
        return Either.right(true);
    }

    public int countUsersInGroup(GroupId groupId) {
        return groupUserDao.countUsersInGroup(groupId);
    }

    private void deactivateGroupUser(GroupId groupId, UserId userId) {
        groupUserDao.getByGroupIdAndUserId(groupId, userId)
            .map(groupUser -> groupUser.deactivate(groupUserDao));
    }

    private void createGroupUser(Group group, User user) {
        groupUserDao.save(new GroupUser(group.id(), user.id(), true));
        groupPersonageStatsService.create(group.id(), user.personageId());
    }
}
