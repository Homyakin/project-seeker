package ru.homyakin.seeker.telegram.group;

import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.ZeroEnabledEventIntervalsError;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.config.GroupConfig;
import ru.homyakin.seeker.telegram.group.database.GroupDao;
import ru.homyakin.seeker.telegram.group.database.GroupMigrateDao;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.GroupSettings;
import ru.homyakin.seeker.telegram.group.models.IncorrectTimeZone;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class GroupService {
    private final GroupDao groupDao;
    private final GroupMigrateDao groupMigrateDao;
    private final GroupStatsService groupStatsService;
    private final GroupConfig groupConfig;

    public GroupService(
        GroupDao groupDao,
        GroupMigrateDao groupMigrateDao,
        GroupStatsService groupStatsService,
        GroupConfig groupConfig
    ) {
        this.groupDao = groupDao;
        this.groupMigrateDao = groupMigrateDao;
        this.groupStatsService = groupStatsService;
        this.groupConfig = groupConfig;
    }

    public Group getOrCreate(GroupId groupId) {
        return getGroup(groupId)
            .map(group -> group.activate(groupDao))
            .orElseGet(() -> createGroup(groupId));
    }

    public void setNotActive(GroupId groupId) {
        getGroup(groupId).map(group -> group.deactivate(groupDao));
    }

    public Group changeLanguage(Group group, Language language) {
        return group.changeLanguage(language, groupDao);
    }

    public List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return groupDao.getGetGroupsWithLessNextEventDate(maxNextEventDate);
    }

    public List<Group> getGetGroupsWithLessNextRumorDate(LocalDateTime maxNextRumorDate) {
        return groupDao.getGetGroupsWithLessNextRumorDate(maxNextRumorDate);
    }

    public void updateNextEventDate(Group group, LocalDateTime nextEventDate) {
        groupDao.updateNextEventDate(group.id(), nextEventDate);
    }

    public void updateNextRumorDate(Group group, LocalDateTime nextRumorDate) {
        groupDao.updateNextRumorDate(group.id(), nextRumorDate);
    }

    public void migrateGroupDate(GroupId from, GroupId to) {
        groupMigrateDao.migrate(from, to);
    }

    public Either<ZeroEnabledEventIntervalsError, Group> toggleEventInterval(Group group, int intervalIndex) {
        return group.toggleEventInterval(intervalIndex)
            .peek(groupDao::update);
    }

    public Either<IncorrectTimeZone, Group> changeTimeZone(Group group, int timeZone) {
        return group.changeTimeZone(timeZone)
            .peek(groupDao::update);
    }

    private Optional<Group> getGroup(GroupId group) {
        return groupDao.getById(group);
    }

    private Group createGroup(GroupId groupId) {
        final var group = new Group(
            groupId,
            true,
            Language.DEFAULT,
            new GroupSettings(
                TimeUtils.moscowOffset(),
                groupConfig.defaultEventIntervals()
            )
        );
        groupDao.save(group);
        groupStatsService.create(groupId);
        return group;
    }
}
