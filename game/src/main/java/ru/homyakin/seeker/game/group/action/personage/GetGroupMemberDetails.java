package ru.homyakin.seeker.game.group.action.personage;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupMemberDetails;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class GetGroupMemberDetails {
    private final GroupPersonageStorage groupPersonageStorage;
    private final PersonageService personageService;

    public GetGroupMemberDetails(
        GroupPersonageStorage groupPersonageStorage,
        PersonageService personageService
    ) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.personageService = personageService;
    }

    public Optional<GroupMemberDetails> execute(GroupId groupId, PersonageId personageId) {
        return groupPersonageStorage
            .findActiveMemberLastOnline(groupId, personageId)
            .flatMap(snapshot -> personageService
                .getById(personageId)
                .map(personage -> new GroupMemberDetails(personage, snapshot))
            );
    }
}
