package ru.homyakin.seeker.game.group.action.personage;

import java.util.Set;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class ActiveGroupPersonagesService {
    private final GroupPersonageStorage storage;

    public ActiveGroupPersonagesService(GroupPersonageStorage storage) {
        this.storage = storage;
    }

    public Set<PersonageId> getActiveGroupPersonages(GroupId groupId) {
        return storage.getActiveGroupPersonages(groupId);
    }

    public boolean isPersonageActiveInGroup(GroupId groupId, PersonageId personageId) {
        return storage.isPersonageActiveInGroup(groupId, personageId);
    }
}
