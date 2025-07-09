package ru.homyakin.seeker.game.personage.models;

import java.util.Optional;
import ru.homyakin.seeker.game.battle.v3.PersonageBattleStats;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;

public record PersonageRaidResult(
    RaidParticipant participant,
    PersonageBattleStats stats,
    Money reward,
    Optional<Item> generatedItem
) {
}
