package ru.homyakin.seeker.telegram.user.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.user.UserDao;
import ru.homyakin.seeker.telegram.user.entity.UserRequest;
import ru.homyakin.seeker.telegram.user.models.User;

@Component
public class CreateUserOrActualizeUsernameCommand {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserDao userDao;
    private final PersonageService personageService;

    public CreateUserOrActualizeUsernameCommand(UserDao userDao, PersonageService personageService) {
        this.userDao = userDao;
        this.personageService = personageService;
    }

    public void execute(UserRequest request) {
        final var existingUser = userDao.getById(request.id());
        if (existingUser.isPresent()) {
            if (!existingUser.get().username().equals(request.username())) {
                userDao.updateUsername(request.id(), request.username());
            }
        } else {
            final var personage = request.originalUsername()
                .map(
                    name -> personageService
                        .createPersonage(name)
                        .fold(
                            _ -> personageService.createPersonage(),
                            success -> success
                        )
                )
                .orElseGet(personageService::createPersonage);
            final var user = new User(
                request.id(),
                false,
                Language.DEFAULT,
                personage.id(),
                request.username()
            );
            userDao.save(user);
            logger.info("Created new user: " + user);
        }
    }
}
