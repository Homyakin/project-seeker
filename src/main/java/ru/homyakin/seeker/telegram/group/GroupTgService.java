package ru.homyakin.seeker.telegram.group;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.ChangeGroupActivity;
import ru.homyakin.seeker.game.group.action.CreateGroup;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.database.GroupDao;
import ru.homyakin.seeker.telegram.group.database.GroupMigrateDao;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class GroupTgService {
    private final CreateGroup createGroup;
    private final ChangeGroupActivity changeGroupActivity;
    private final GroupDao groupDao;
    private final GroupMigrateDao groupMigrateDao;
    private final GroupStatsService groupStatsService;

    public GroupTgService(
        CreateGroup createGroup,
        ChangeGroupActivity changeGroupActivity,
        GroupDao groupDao,
        GroupMigrateDao groupMigrateDao,
        GroupStatsService groupStatsService
    ) {
        this.createGroup = createGroup;
        this.changeGroupActivity = changeGroupActivity;
        this.groupDao = groupDao;
        this.groupMigrateDao = groupMigrateDao;
        this.groupStatsService = groupStatsService;
    }

    public GroupTg forceGet(GroupId groupId) {
        return groupDao.get(groupId).orElseThrow();
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

    public long getActiveGroupsCount() {
        return groupDao.getActiveGroupsCount();
    }

    private Optional<GroupTg> getGroup(GroupTgId group) {
        return groupDao.getById(group);
    }

    private GroupTg createGroup(GroupTgId groupId) {
        final var group = createGroup.create(TimeUtils.moscowTime());
        final var groupTg = new GroupTg(
            groupId,
            Language.DEFAULT,
            group.id()
        );
        groupDao.save(groupTg);
        groupStatsService.create(groupId);
        return groupTg;
    }
}
