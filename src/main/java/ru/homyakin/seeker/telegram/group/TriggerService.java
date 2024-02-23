package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.group.database.TriggerDao;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.Trigger;
import ru.homyakin.seeker.telegram.group.models.TriggerError;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

@Service
public class TriggerService {

    private final TriggerDao triggerDao;

    public TriggerService(TriggerDao triggerDao) {
        this.triggerDao = triggerDao;
    }

    //TODO разобраться, приделать Either

    public Optional<Trigger> getTrigger(Group group, String textToTrigger) {
        Trigger triggerRequest = Trigger.from(group, textToTrigger, null);
        return triggerDao.getTrigger(triggerRequest);
    }

    public Either<TriggerError, Success> createOrReplaceTrigger(Group group, String textToTrigger, String triggerText) {
        triggerDao.createOrUpdate(Trigger.from(group, textToTrigger, triggerText));
        return Either.right(Success.INSTANCE);
    }
}
