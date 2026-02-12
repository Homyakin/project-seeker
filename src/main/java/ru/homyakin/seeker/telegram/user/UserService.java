package ru.homyakin.seeker.telegram.user;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.models.UserType;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.models.UserId;

@Component
public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User forceGetFromGroup(UserId userId) {
        return userDao.getById(userId).orElseThrow();
    }

    public Optional<User> getByMention(MentionInfo mentionInfo, GroupTgId groupId) {
        assert mentionInfo.userType() == UserType.USER;

        return switch (mentionInfo) {
            case MentionInfo.Id id -> Optional.of(forceGetFromGroup(id.userId()));
            case MentionInfo.UsernameMention usernameMention -> userDao.getByUsernameInGroup(usernameMention.username(), groupId);
        };
    }

    public Optional<User> getByMention(MentionInfo mentionInfo) {
        assert mentionInfo.userType() == UserType.USER;

        return switch (mentionInfo) {
            case MentionInfo.Id id -> Optional.of(forceGetFromGroup(id.userId()));
            case MentionInfo.UsernameMention usernameMention -> userDao.getByUsername(usernameMention.username());
        };
    }

    public User forceGetFromPrivate(UserId userId) {
        return userDao
            .getById(userId)
            .map(user -> user.activatePrivateMessages(userDao))
            .orElseThrow();
    }

    public User changeLanguage(User user, Language language) {
        return user.changeLanguage(language, userDao);
    }

    public User getByPersonageIdForce(PersonageId personageId) {
        return userDao.getByPersonageId(personageId)
            .orElseThrow(() -> new IllegalStateException("User must be present at personage " + personageId));
    }

    public void deactivatePrivateMessages(UserId userId) {
        userDao.getById(userId).ifPresent(it -> it.deactivatePrivateMessages(userDao));
    }
}
