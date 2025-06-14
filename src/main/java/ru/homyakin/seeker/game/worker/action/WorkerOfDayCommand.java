package ru.homyakin.seeker.game.worker.action;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.personage.CountPersonagesInGroup;
import ru.homyakin.seeker.game.group.action.personage.RandomGroupPersonage;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.worker.entity.WorkerOfDayConfig;
import ru.homyakin.seeker.game.worker.entity.WorkerOfDayResult;
import ru.homyakin.seeker.game.worker.entity.WorkerOfDayStorage;
import ru.homyakin.seeker.game.worker.error.WorkerOfDayError;
import ru.homyakin.seeker.game.stats.action.GroupPersonageStatsService;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;

@Component
public class WorkerOfDayCommand {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GetGroup getGroup;
    private final PersonageService personageService;
    private final RandomGroupPersonage randomGroupPersonage;
    private final CountPersonagesInGroup countPersonagesInGroup;
    private final GroupPersonageStatsService groupPersonageStatsService;
    private final WorkerOfDayConfig config;
    private final WorkerOfDayStorage storage;

    public WorkerOfDayCommand(
        GetGroup getGroup,
        PersonageService personageService,
        RandomGroupPersonage randomGroupPersonage,
        CountPersonagesInGroup countPersonagesInGroup,
        GroupPersonageStatsService groupPersonageStatsService,
        WorkerOfDayConfig config,
        WorkerOfDayStorage storage
    ) {
        this.getGroup = getGroup;
        this.personageService = personageService;
        this.randomGroupPersonage = randomGroupPersonage;
        this.countPersonagesInGroup = countPersonagesInGroup;
        this.groupPersonageStatsService = groupPersonageStatsService;
        this.config = config;
        this.storage = storage;
    }

    public Either<WorkerOfDayError, WorkerOfDayResult> chooseInGroup(GroupId groupId) {
        final var group = getGroup.forceGet(groupId);
        if (!group.isRegistered()) {
            return Either.left(WorkerOfDayError.NotRegisteredGroup.INSTANCE);
        }
        final var date = TimeUtils.moscowDate();
        final var todayResult = storage.findPersonageIdByGroupIdAndDate(groupId, date);
        if (todayResult.isPresent()) {
            return Either.left(new WorkerOfDayError.AlreadyChosen(todayResult.get()));
        }
        final var count = countPersonagesInGroup.count(groupId);
        if (config.minimumMembers() > count) {
            return Either.left(new WorkerOfDayError.NotEnoughUsers(config.minimumMembers()));
        }

        final var result = randomGroupPersonage.randomMember(groupId);
        if (result.isLeft()) {
            return Either.left(WorkerOfDayError.InternalError.INSTANCE);
        }
        if (result.get().isEmpty()) {
            return Either.left(new WorkerOfDayError.NotEnoughUsers(config.minimumMembers()));
        }
        final var personageId = result.get().get();
        logger.info("Personage {} was selected in group {} worker", personageId.value(), groupId.value());
        storage.save(groupId, personageId, date);
        groupPersonageStatsService.incrementWorkerOfDay(groupId, personageId);
        final var effect = createEffect();
        final var personage = personageService.addEffect(personageId, PersonageEffectType.WORKER_OF_DAY_EFFECT, effect);
        return Either.right(new WorkerOfDayResult(personage, effect.effect()));
    }

    private PersonageEffect createEffect() {
        return new PersonageEffect(
            new Effect.Multiplier(10, EffectCharacteristic.HEALTH),
            TimeUtils.moscowTime().plus(Duration.ofHours(24))
        );
    }
}
