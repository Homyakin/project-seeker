package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;

public interface OutpostStorage {
    List<OutpostSlot.BuildingSlot> listBuildingSlots(GroupId groupId);

    Optional<OutpostSlot.BuildingSlot> findBuildingSlot(GroupId groupId, Building building);

    boolean tryInsertWithProgress(
        GroupId groupId,
        Building building,
        int level,
        OutpostBuildingProgress progress
    );

    boolean trySetProgress(GroupId groupId, Building building, OutpostBuildingProgress progress);

    boolean updateBuildingProgress(GroupId groupId, Building building, OutpostBuildingProgress progress);

    boolean completeInProgressBuilding(GroupId groupId, Building building);
}
