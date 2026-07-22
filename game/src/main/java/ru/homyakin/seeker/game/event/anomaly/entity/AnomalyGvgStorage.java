package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;

public interface AnomalyGvgStorage {
    int getRating(GroupId groupId);

    void updateRating(GroupId groupId, int newRating);

    void saveRecentOpponent(GroupId groupA, GroupId groupB, LocalDateTime foughtAt);

    Optional<LocalDateTime> findRecentOpponentFoughtAt(GroupId groupA, GroupId groupB);

    List<GroupId> findEligibleChallengeTargets(GroupId excludeGroupId);
}
