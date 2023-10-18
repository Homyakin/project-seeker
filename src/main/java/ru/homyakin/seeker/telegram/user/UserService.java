package ru.homyakin.seeker.telegram.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.User;

import java.util.Optional;

@Component
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;
    private final PersonageService personageService;

    public UserService(
        UserDao userDao,
        PersonageService personageService
    ) {
        this.userDao = userDao;
        this.personageService = personageService;
    }

    public User getOrCreateFromGroup(Long userId) {
        return userDao
            .getById(userId)
            .orElseGet(() -> createUser(userId, false));
    }

    public Optional<User> tryGetOrCreateByMention(MentionInfo mentionInfo, long groupId) {
        return switch (mentionInfo) {
            case MentionInfo.Id id -> Optional.of(getOrCreateFromGroup(id.userId()));
            case MentionInfo.Username username -> userDao.getByUsernameInGroup(username.username(), groupId);
        };
    }

    public User getOrCreateFromPrivate(Long userId) {
        return userDao
            .getById(userId)
            .map(user -> user.activatePrivateMessages(userDao))
            .orElseGet(() -> createUser(userId, true));
    }

    public User changeLanguage(User user, Language language) {
        return user.changeLanguage(language, userDao);
    }

    public User getByPersonageIdForce(long personageId) {
        return userDao.getByPersonageId(personageId)
            .orElseThrow(() -> new IllegalStateException("User must be present at personage " + personageId));
    }

    public void updateUserInfoFromUpdate(Update update) {
        if (update.hasMessage()) {
            updateUserInfoFromUser(update.getMessage().getFrom());
        } else if (update.hasCallbackQuery()) {
            updateUserInfoFromUser(update.getCallbackQuery().getFrom());
        } else if (update.hasChatMember()) {
            if (update.getChatMember().getNewChatMember() != null) {
                updateUserInfoFromUser(update.getChatMember().getNewChatMember().getUser());
            }
        }
    }

    private void updateUserInfoFromUser(org.telegram.telegrambots.meta.api.objects.User user) {
        if (user.getIsBot()) {
            return;
        }
        final var username = Optional.ofNullable(user.getUserName());
        final var savedUser = userDao
            .getById(user.getId())
            .orElseGet(() -> createUser(user.getId(), false, username));
        if (!savedUser.username().equals(username)) {
            userDao.updateUsername(savedUser.id(), username.orElse(null));
        }
    }

    private User createUser(long userId, boolean isPrivateMessage) {
        return createUser(userId, isPrivateMessage, Optional.empty());
    }

    private User createUser(long userId, boolean isPrivateMessage, Optional<String> username) {
        final var personage = username
            .map(
                name -> personageService
                    .createPersonage(name)
                    .fold(
                        error -> personageService.createPersonage(),
                        success -> success
                    )
            )
            .orElseGet(personageService::createPersonage);
        final var user = new User(
            userId,
            isPrivateMessage,
            Language.DEFAULT,
            personage.id(),
            username
        );
        userDao.save(user);
        return user;
    }
}
