package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.List;
import ru.homyakin.seeker.game.item.ItemObject;

public record ItemObjects(
    List<ItemObject> object
) {
}