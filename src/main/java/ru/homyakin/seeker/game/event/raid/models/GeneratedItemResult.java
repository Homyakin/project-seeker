package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.models.Personage;

public sealed interface GeneratedItemResult {
    default RaidItem toRaidItem() {
        return switch (this) {
            case Success s -> new RaidItem.ItemDrop(s.item());
            case NotEnoughSpaceInBag n -> new RaidItem.ItemDrop(n.item());
            case ContrabandDrop c -> new RaidItem.ContrabandDrop(c.contraband());
        };
    }

    record Success(Personage personage, Item item) implements GeneratedItemResult {}

    record NotEnoughSpaceInBag(Personage personage, Item item) implements GeneratedItemResult {}

    record ContrabandDrop(Personage personage, Contraband contraband) implements GeneratedItemResult {}
}
