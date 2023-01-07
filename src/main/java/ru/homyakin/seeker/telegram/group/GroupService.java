package ru.homyakin.seeker.telegram.group;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.database.GroupDao;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class GroupService {
    private final GroupDao groupDao;

    public GroupService(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    public Group getOrCreate(long groupId) {
        return getGroup(groupId)
            .map(group -> group.activate(groupDao))
            .orElseGet(() -> createGroup(groupId));
    }

    public void setNotActive(long groupId) {
        getGroup(groupId).map(group -> group.deactivate(groupDao));
    }

    public Group changeLanguage(Group group, Language language) {
        return group.changeLanguage(language, groupDao);
    }

    public List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return groupDao.getGetGroupsWithLessNextEventDate(maxNextEventDate);
    }

    public void updateNextEventDate(Group group, LocalDateTime nextEventDate) {
        group.updateNextEventDate(nextEventDate, groupDao);
    }

    private Optional<Group> getGroup(Long group) {
        return groupDao.getById(group);
    }

    private Group createGroup(Long groupId) {
        final var group = new Group(groupId, true, Language.DEFAULT, TimeUtils.moscowTime());
        groupDao.save(group);
        return group;
    }
    
}
