package ru.homyakin.seeker.game.group.action.personage;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Set;

@Component
public class GetActiveGroupPersonagesCommand {
    private final GroupPersonageStorage storage;

    public GetActiveGroupPersonagesCommand(GroupPersonageStorage storage) {
        this.storage = storage;
    }

    public Set<PersonageId> execute(GroupId groupId) {
        return storage.getActiveGroupPersonages(groupId);
    }
}
