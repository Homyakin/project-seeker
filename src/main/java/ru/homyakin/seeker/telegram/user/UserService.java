package ru.homyakin.seeker.telegram.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.user.models.User;

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

    private User createUser(Long userId, boolean isPrivateMessage) {
        final var personage = personageService.createPersonage();
        final var user = new User(
            userId,
            isPrivateMessage,
            Language.DEFAULT,
            personage.id()
        );
        userDao.save(user);
        return user;
    }
}
