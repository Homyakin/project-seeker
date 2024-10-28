package ru.homyakin.seeker.game.group.action.personage;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class CreateOrActivateGroupPersonageCommand {
    private final GroupPersonageStorage storage;

    public CreateOrActivateGroupPersonageCommand(GroupPersonageStorage storage) {
        this.storage = storage;
    }

    public void execute(GroupId groupId, PersonageId personageId) {
        storage.createOrActivate(groupId, personageId);
    }
}