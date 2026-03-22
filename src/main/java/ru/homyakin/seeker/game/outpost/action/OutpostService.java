package ru.homyakin.seeker.game.outpost.action;

import java.util.List;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;

@Component
public class OutpostService {
    private final OutpostStorage storage;

    public OutpostService(OutpostStorage storage) {
        this.storage = storage;
    }

    public List<OutpostSlot> listSlots(GroupId groupId) {
        final var buildings = storage.listBuildingSlots(groupId);
        if (buildings.isEmpty()) {
            return List.of(OutpostSlot.EmptySlot.INSTANCE);
        }
        return buildings.stream().map(slot -> (OutpostSlot) slot).toList();
    }
}
