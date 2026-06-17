package ru.homyakin.seeker.telegram.user.state;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.user.models.User;

import java.util.Optional;
import ru.homyakin.seeker.telegram.user.models.UserId;

@Service
public class UserStateService {
    private final UserStateDao userStateDao;
    
    public UserStateService(UserStateDao userStateDao) {
        this.userStateDao = userStateDao;
    }

    public Optional<UserState> getUserStateById(UserId userId) {
        return userStateDao.getUserStateById(userId);
    }

    public void setUserState(User user, UserState state) {
        userStateDao.setUserState(user.id(), state);
    }

    public void clearUserState(User user) {
        userStateDao.clearUserState(user.id());
    }
}
