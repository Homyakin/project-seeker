package ru.homyakin.seeker.game.item.models;

import java.util.List;

public record PutOnItemResult(
    PersonageItem item,
    List<PersonageItem> takenOffItems
) {
}
