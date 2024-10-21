package ru.homyakin.seeker.game.spin.action;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CountPersonagesInGroup;
import ru.homyakin.seeker.game.group.action.personage.RandomGroupPersonage;
import ru.homyakin.seeker.game.group.action.personage.SpinStats;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.spin.entity.EverydaySpinConfig;
import ru.homyakin.seeker.game.spin.entity.EverydaySpinStorage;
import ru.homyakin.seeker.game.spin.error.SpinError;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class ChooseRandomPersonage {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RandomGroupPersonage randomGroupPersonage;
    private final CountPersonagesInGroup countPersonagesInGroup;
    private final SpinStats spinStats;
    private final EverydaySpinConfig config;
    private final EverydaySpinStorage storage;

    public ChooseRandomPersonage(
        RandomGroupPersonage randomGroupPersonage,
        CountPersonagesInGroup countPersonagesInGroup,
        SpinStats spinStats,
        EverydaySpinConfig config,
        EverydaySpinStorage storage
    ) {
        this.randomGroupPersonage = randomGroupPersonage;
        this.countPersonagesInGroup = countPersonagesInGroup;
        this.spinStats = spinStats;
        this.config = config;
        this.storage = storage;
    }

    public Either<SpinError, PersonageId> chooseInGroup(GroupId groupId) {
        final var date = TimeUtils.moscowDate();
        final var todayResult = storage.findPersonageIdByGroupIdAndDate(groupId, date);
        if (todayResult.isPresent()) {
            return Either.left(new SpinError.AlreadyChosen(todayResult.get()));
        }
        final var count = countPersonagesInGroup.count(groupId);
        if (config.minimumUsers() > count) {
            return Either.left(new SpinError.NotEnoughUsers(config.minimumUsers()));
        }

        final var result = randomGroupPersonage.random(groupId);
        if (result.isLeft()) {
            return Either.left(SpinError.InternalError.INSTANCE);
        }
        if (result.get().isEmpty()) {
            return Either.left(new SpinError.NotEnoughUsers(config.minimumUsers()));
        }
        final var personageId = result.get().get();
        logger.info("Personage {} was selected in group {} spin", personageId.value(), groupId.value());
        storage.save(groupId, personageId, date);
        spinStats.addPersonageSpinWin(groupId, personageId);
        return Either.right(personageId);
    }
}
