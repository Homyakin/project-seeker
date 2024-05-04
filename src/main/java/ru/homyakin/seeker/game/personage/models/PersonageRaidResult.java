package ru.homyakin.seeker.game.personage.models;

import java.util.Optional;
import ru.homyakin.seeker.game.battle.PersonageBattleStats;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;

public record PersonageRaidResult(
    Personage personage,
    PersonageBattleStats stats,
    Money reward,
    Optional<Item> generatedItem
) {
}
