package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import ru.homyakin.seeker.game.battle.PersonageBattleStats;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

public record PersonageWorldRaidBattleResult(
    Personage personage,
    PersonageBattleStats stats,
    Money reward,
    Optional<Item> generatedItem
) {
    public PersonageId personageId() {
        return personage.id();
    }
}
