package ru.homyakin.seeker.telegram.group;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.ChangeGroupActivity;
import ru.homyakin.seeker.game.group.action.CreateGroupCommand;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.database.GroupDao;
import ru.homyakin.seeker.telegram.group.database.GroupMigrateDao;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class GroupTgService {
    private final CreateGroupCommand createGroupCommand;
    private final ChangeGroupActivity changeGroupActivity;
    private final GroupDao groupDao;
    private final GroupMigrateDao groupMigrateDao;

    public GroupTgService(
        CreateGroupCommand createGroupCommand,
        ChangeGroupActivity changeGroupActivity,
        GroupDao groupDao,
        GroupMigrateDao groupMigrateDao
    ) {
        this.createGroupCommand = createGroupCommand;
        this.changeGroupActivity = changeGroupActivity;
        this.groupDao = groupDao;
        this.groupMigrateDao = groupMigrateDao;
    }

    public GroupTg forceGet(GroupId groupId) {
        return groupDao.getByDomainId(groupId).orElseThrow();
    }

    public GroupTg getOrCreate(GroupTgId groupId) {
        final var optionalGroup = getGroup(groupId);
        optionalGroup.ifPresent(group -> changeGroupActivity.activate(group.domainGroupId()));
        return optionalGroup.orElseGet(() -> createGroup(groupId));
    }

    public void setNotActive(GroupTgId groupId) {
        getGroup(groupId).ifPresent(group -> changeGroupActivity.deactivate(group.domainGroupId()));
    }

    public GroupTg changeLanguage(GroupTg group, Language language) {
        return group.changeLanguage(language, groupDao);
    }

    public void migrateGroupData(GroupTgId from, GroupTgId to) {
        groupMigrateDao.migrate(from, to);
    }

    private Optional<GroupTg> getGroup(GroupTgId group) {
        return groupDao.getById(group);
    }

    private GroupTg createGroup(GroupTgId groupId) {
        final var group = createGroupCommand.execute(TimeUtils.moscowTime());
        final var groupTg = new GroupTg(
            groupId,
            Language.DEFAULT,
            group.id()
        );
        groupDao.save(groupTg);
        return groupTg;
    }
}
