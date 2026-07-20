package ru.homyakin.seeker.game.item.loadout.entity;

import java.util.List;
import java.util.Set;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record EquipmentLoadout(
    long id,
    PersonageId personageId,
    String name,
    List<Long> itemIds,
    Position battlePosition,
    Set<EventType> defaultEventTypes
) {
    public boolean isDefaultFor(EventType eventType) {
        return defaultEventTypes.contains(eventType);
    }
}
