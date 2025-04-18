package ru.homyakin.seeker.test_utils;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.PersonageMemberGroup;

import java.time.LocalDateTime;
import java.util.Optional;

public class PersonageMemberGroupUtils {
    public static PersonageMemberGroup empty() {
        return new PersonageMemberGroup(
            Optional.empty(),
            Optional.empty()
        );
    }

    public static PersonageMemberGroup withGroup(GroupId groupId) {
        return new PersonageMemberGroup(
            Optional.of(groupId),
            Optional.empty()
        );
    }

    public static PersonageMemberGroup withDate(LocalDateTime lastLeaveGroup) {
        return new PersonageMemberGroup(
            Optional.empty(),
            Optional.of(lastLeaveGroup)
        );
    }
}
