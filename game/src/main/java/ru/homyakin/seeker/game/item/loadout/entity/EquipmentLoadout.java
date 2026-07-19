package ru.homyakin.seeker.game.item.loadout.entity;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record EquipmentLoadout(
    long id,
    PersonageId personageId,
    String name,
    List<Long> itemIds
) {
}
