package ru.homyakin.seeker.game.group.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface GroupTaxStorage {
    GroupTaxInfo loadTaxRow(GroupId groupId);

    GroupTaxInfo lockTax(GroupId groupId);

    int countLeaved(GroupId groupId);

    void deleteOldestLeaved(GroupId groupId, int deleteCount);

    void updateTaxRow(GroupId groupId, int effectiveTax, Optional<LocalDateTime> lastTaxUpdate);

    boolean deleteLeavedIfExists(GroupId groupId, PersonageId personageId);

    void deleteAllLeaved(GroupId groupId);

    void insertLeaved(GroupId groupId, PersonageId personageId, LocalDateTime leavedAt);

    List<GroupId> findGroupIdsDueForTaxUpdate(LocalDateTime cutoff);
}
