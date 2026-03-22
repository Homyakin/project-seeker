package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;
import ru.homyakin.seeker.common.models.GroupId;

public interface OutpostStorage {
    List<OutpostSlot.BuildingSlot> listBuildingSlots(GroupId groupId);
}
