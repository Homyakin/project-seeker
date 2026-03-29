package ru.homyakin.seeker.game.outpost.entity;

import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;

public sealed interface OutpostSlot {

    record BuildingSlot(
        GroupId groupId,
        Building building,
        int level,
        Optional<OutpostBuildingProgress> progress
    ) implements OutpostSlot {
    }

    enum EmptySlot implements OutpostSlot {
        INSTANCE
    }
}
