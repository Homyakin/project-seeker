package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.item.models.LegacyItem;
import ru.homyakin.seeker.game.item.models.PersonageItem;

public sealed interface RaidItem {
    record ItemDrop(PersonageItem item) implements RaidItem {}

    record LegacyItemDrop(LegacyItem item) implements RaidItem {}

    record ContrabandDrop(Contraband contraband) implements RaidItem {}
}
