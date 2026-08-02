package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ru.homyakin.seeker.common.models.GroupId;

public interface AnomalyGvgStorage {
    int getRating(GroupId groupId);

    void updateRating(GroupId groupId, int newRating);

    List<LocalDateTime> findOpponentFoughtAtList(GroupId groupA, GroupId groupB);

    List<GroupId> findEligibleChallengeTargets(GroupId excludeGroupId, LocalDate day);
}
