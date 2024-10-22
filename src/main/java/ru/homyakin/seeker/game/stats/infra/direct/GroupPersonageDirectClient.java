package ru.homyakin.seeker.game.stats.infra.direct;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CreateGroupPersonageCommand;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageClient;

@Component
public class GroupPersonageDirectClient implements GroupPersonageClient {
    private final CreateGroupPersonageCommand createGroupPersonageCommand;

    public GroupPersonageDirectClient(CreateGroupPersonageCommand createGroupPersonageCommand) {
        this.createGroupPersonageCommand = createGroupPersonageCommand;
    }

    @Override
    public void create(GroupId groupId, PersonageId personageId) {
        createGroupPersonageCommand.execute(groupId, personageId);
    }
}
