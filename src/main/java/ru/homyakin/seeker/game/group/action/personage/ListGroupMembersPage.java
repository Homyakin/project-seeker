package ru.homyakin.seeker.game.group.action.personage;

import java.util.List;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupMembersPageResult;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.infra.config.GroupMembersListConfig;
import ru.homyakin.seeker.game.online.OnlineTypeProvider;

@Component
public class ListGroupMembersPage {
    private final GroupStorage groupStorage;
    private final GroupPersonageStorage groupPersonageStorage;
    private final GroupMembersListConfig membersListConfig;
    private final OnlineTypeProvider onlineTypeProvider;

    public ListGroupMembersPage(
        GroupPersonageStorage groupPersonageStorage,
        GroupStorage groupStorage,
        GroupMembersListConfig membersListConfig,
        OnlineTypeProvider onlineTypeProvider
    ) {
        this.groupStorage = groupStorage;
        this.groupPersonageStorage = groupPersonageStorage;
        this.membersListConfig = membersListConfig;
        this.onlineTypeProvider = onlineTypeProvider;
    }

    public GroupMembersPageResult execute(GroupId groupId, int page) {
        final var pageSize = membersListConfig.pageSize();
        final var total = groupStorage.memberCount(groupId);
        if (total == 0) {
            return new GroupMembersPageResult(
                List.of(),
                0,
                0,
                0,
                0,
                0,
                onlineTypeProvider::convertDuration
            );
        }
        final var totalPages = (total + pageSize - 1) / pageSize;
        page = Math.min(Math.max(1, page), totalPages);
        final var offset = (page - 1) * pageSize;
        final var slice = groupPersonageStorage.listMembersOrderedByPersonageId(groupId, offset, pageSize);
        return new GroupMembersPageResult(
            slice,
            total,
            offset + 1,
            offset + slice.size(),
            totalPages,
            page,
            onlineTypeProvider::convertDuration
        );
    }
}
