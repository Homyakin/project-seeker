package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.group.database.EverydaySpinDao;
import ru.homyakin.seeker.telegram.group.models.SpinCount;
import ru.homyakin.seeker.telegram.group.models.SpinError;
import ru.homyakin.seeker.utils.TimeUtils;

//TODO если появятся клиенты кроме телеги, надо подумать как это объединить
@Service
public class EverydaySpinService {
    private final GroupUserService groupUserService;
    private final EverydaySpinDao everydaySpinDao;
    private final int minimumUsers;

    public EverydaySpinService(
        EverydaySpinConfig config,
        GroupUserService groupUserService,
        EverydaySpinDao everydaySpinDao
    ) {
        this.minimumUsers = config.minimumUsers();
        this.groupUserService = groupUserService;
        this.everydaySpinDao = everydaySpinDao;
    }

    public Either<SpinError, Long> chooseRandomUserId(long groupId) {
        final var date = TimeUtils.moscowDate();
        final var todayResult = everydaySpinDao.findUserIdByGrouptgIdAndDate(groupId, date);
        if (todayResult.isPresent()) {
            return Either.left(new SpinError.AlreadyChosen(todayResult.get()));
        }
        final var count = groupUserService.countUsersInGroup(groupId);
        if (minimumUsers > count) {
            return Either.left(new SpinError.NotEnoughUsers(minimumUsers));
        }

        final var user = groupUserService.getRandomUserFromGroup(groupId);
        if (user.isEmpty()) {
            return Either.left(new SpinError.NotEnoughUsers(minimumUsers));
        }
        everydaySpinDao.save(groupId, user.get().userId(), date);
        return Either.right(user.get().userId());
    }

    public SpinCount getSpinCountForGroup(long groupId) {
        return new SpinCount(everydaySpinDao.findPersonageCountByGrouptgId(groupId));
    }
}
