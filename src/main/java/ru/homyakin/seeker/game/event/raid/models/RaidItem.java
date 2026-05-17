package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.item.models.LegacyItem;

public sealed interface RaidItem {
    record ItemDrop(LegacyItem item) implements RaidItem {}

    record ContrabandDrop(Contraband contraband) implements RaidItem {}
}
