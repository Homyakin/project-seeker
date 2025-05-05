package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.homyakin.seeker.common.models.Error;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupPersonage;
import ru.homyakin.seeker.game.group.action.personage.CreateOrActivateGroupPersonageCommand;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.ChatMemberError;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.models.Pair;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;

@Service
public class GroupUserService implements CheckGroupPersonage {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GroupTgService groupTgService;
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final CreateOrActivateGroupPersonageCommand createOrActivateGroupPersonage;

    public GroupUserService(
        GroupTgService groupTgService,
        UserService userService,
        TelegramSender telegramSender,
        CreateOrActivateGroupPersonageCommand createOrActivateGroupPersonage
    ) {
        this.groupTgService = groupTgService;
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.createOrActivateGroupPersonage = createOrActivateGroupPersonage;
    }

    public boolean isUserAdminInGroup(GroupTgId groupId, UserId userId) {
        return telegramSender.send(TelegramMethods.createGetChatMember(groupId, userId))
            .fold(
                _ -> false,
                it -> it instanceof ChatMemberAdministrator || it instanceof ChatMemberOwner
            );
    }

    public Pair<GroupTg, User> getAndActivateOrCreate(GroupTgId groupId, UserId userId) {
        final var group = groupTgService.getOrCreate(groupId);
        final var user = userService.forceGetFromGroup(userId);
        createOrActivateGroupPersonage.execute(group.domainGroupId(), user.personageId());
        return Pair.of(group, user);
    }

    @Override
    public Either<Error, Boolean> stillInGroup(GroupId groupId, PersonageId personageId) {
        final var user = userService.getByPersonageIdForce(personageId);
        final var groupTg = groupTgService.forceGet(groupId);
        final var result =  telegramSender.send(TelegramMethods.createGetChatMember(groupTg.id(), user.id()));
        if (result.isLeft()) {
            return switch (result.getLeft()) {
                case ChatMemberError.UserNotFound _, ChatMemberError.InvalidParticipant _ -> {
                    logger.warn("User {} is no longer in group {}", user.id().value(), groupId.value());
                    yield Either.right(false);
                }
                case ChatMemberError.InternalError _ -> Either.left(Error.INSTANCE);
            };
        }
        return Either.right(true);
    }

    @Override
    public boolean isAdminInGroup(GroupId groupId, PersonageId personageId) {
        final var groupTg = groupTgService.forceGet(groupId);
        return isUserAdminInGroup(groupTg.id(), userService.getByPersonageIdForce(personageId).id());
    }
}
