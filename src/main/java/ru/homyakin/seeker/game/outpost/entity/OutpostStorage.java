package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;

public interface OutpostStorage {
    List<OutpostSlot.BuildingSlot> listBuildingSlots(GroupId groupId);

    Optional<OutpostSlot.BuildingSlot> findBuildingSlot(GroupId groupId, Building building);

    /**
     * @return {@code true} если вставлена новая строка
     */
    boolean tryInsertWithProgress(
        GroupId groupId,
        Building building,
        int level,
        OutpostBuildingProgress progress
    );

    /**
     * Starts upgrade construction: sets {@code progress} while level stays unchanged until completion.
     *
     * @return {@code true} если строка обновлена (ранее не было progress)
     */
    boolean trySetProgress(GroupId groupId, Building building, OutpostBuildingProgress progress);

    /**
     * @return {@code true} если строка с непустым progress обновлена
     */
    boolean updateBuildingProgress(GroupId groupId, Building building, OutpostBuildingProgress progress);

    /**
     * Завершает стройку: level + 1, progress = NULL.
     *
     * @return {@code true} если строка была в процессе стройки
     */
    boolean completeInProgressBuilding(GroupId groupId, Building building);
}
