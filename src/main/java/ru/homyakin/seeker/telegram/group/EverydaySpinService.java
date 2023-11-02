package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.group.database.EverydaySpinDao;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.SpinCount;
import ru.homyakin.seeker.telegram.group.models.SpinError;
import ru.homyakin.seeker.telegram.group.stats.GroupPersonageStatsService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.utils.TimeUtils;

//TODO если появятся клиенты кроме телеги, надо подумать как это объединить
@Service
public class EverydaySpinService {
    private static final Logger logger = LoggerFactory.getLogger(EverydaySpinService.class);
    private final GroupUserService groupUserService;
    private final EverydaySpinDao everydaySpinDao;
    private final GroupPersonageStatsService groupPersonageStatsService;
    private final int minimumUsers;

    public EverydaySpinService(
        EverydaySpinConfig config,
        GroupUserService groupUserService,
        EverydaySpinDao everydaySpinDao,
        GroupPersonageStatsService groupPersonageStatsService
    ) {
        this.minimumUsers = config.minimumUsers();
        this.groupUserService = groupUserService;
        this.everydaySpinDao = everydaySpinDao;
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    public Either<SpinError, User> chooseRandomUser(GroupId groupId) {
        final var date = TimeUtils.moscowDate();
        final var todayResult = everydaySpinDao.findPersonageIdByGrouptgIdAndDate(groupId, date);
        if (todayResult.isPresent()) {
            return Either.left(new SpinError.AlreadyChosen(todayResult.get()));
        }
        final var count = groupUserService.countUsersInGroup(groupId);
        if (minimumUsers > count) {
            return Either.left(new SpinError.NotEnoughUsers(minimumUsers));
        }

        User user = null;
        do {
            final var randomUser = groupUserService.getRandomUserFromGroup(groupId);
            if (randomUser.isEmpty()) {
                return Either.left(new SpinError.NotEnoughUsers(minimumUsers));
            }
            final var result = groupUserService.isUserStillInGroup(groupId, randomUser.get().id());
            if (result.isRight() && result.get()) {
                user = randomUser.get();
            } else if (result.isLeft()) {
                return Either.left(SpinError.InternalError.INSTANCE);
            }
        } while (user == null);
        logger.info("Personage {} was selected in spin", user.personageId().value());
        everydaySpinDao.save(groupId, user.personageId(), date);
        groupPersonageStatsService.addSpinWin(groupId, user.personageId());
        return Either.right(user);
    }

    public SpinCount getSpinCountForGroup(GroupId groupId) {
        return new SpinCount(everydaySpinDao.findPersonageCountByGrouptgId(groupId));
    }
}
