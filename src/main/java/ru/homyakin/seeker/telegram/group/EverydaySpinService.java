package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.group.database.EverydaySpinDao;
import ru.homyakin.seeker.telegram.group.models.GroupUser;
import ru.homyakin.seeker.telegram.group.models.SpinCount;
import ru.homyakin.seeker.telegram.group.models.SpinError;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.TimeUtils;

//TODO если появятся клиенты кроме телеги, надо подумать как это объединить
@Service
public class EverydaySpinService {
    private static final Logger logger = LoggerFactory.getLogger(EverydaySpinService.class);
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

    public Either<SpinError, UserId> chooseRandomUserId(long groupId) {
        final var date = TimeUtils.moscowDate();
        final var todayResult = everydaySpinDao.findUserIdByGrouptgIdAndDate(groupId, date);
        if (todayResult.isPresent()) {
            return Either.left(new SpinError.AlreadyChosen(todayResult.get()));
        }
        final var count = groupUserService.countUsersInGroup(groupId);
        if (minimumUsers > count) {
            return Either.left(new SpinError.NotEnoughUsers(minimumUsers));
        }

        GroupUser groupUser = null;
        do {
            final var randomGroupUser = groupUserService.getRandomUserFromGroup(groupId);
            if (randomGroupUser.isEmpty()) {
                return Either.left(new SpinError.NotEnoughUsers(minimumUsers));
            }
            final var result = groupUserService.isUserStillInGroup(randomGroupUser.get());
            if (result.isRight() && result.get()) {
                groupUser = randomGroupUser.get();
            } else if (result.isLeft()) {
                return Either.left(SpinError.InternalError.INSTANCE);
            }
        } while (groupUser == null);
        logger.info("User {} was selected in spin", groupUser.userId());
        everydaySpinDao.save(groupId, groupUser.userId(), date);
        return Either.right(groupUser.userId());
    }

    public SpinCount getSpinCountForGroup(long groupId) {
        return new SpinCount(everydaySpinDao.findPersonageCountByGrouptgId(groupId));
    }
}
