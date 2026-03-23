package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;
import ru.homyakin.seeker.common.models.GroupId;

public interface OutpostStorage {
    List<OutpostSlot.BuildingSlot> listBuildingSlots(GroupId groupId);

    /**
     * @return {@code true} если вставлена новая строка
     */
    boolean tryInsert(GroupId groupId, Building building, int level);

    /**
     * @return {@code true} если строка обновлена
     */
    boolean incrementLevel(GroupId groupId, Building building);
}
