package ru.homyakin.seeker.game.personage.models;

import java.util.Optional;
import ru.homyakin.seeker.game.battle.PersonageBattleStats;
import ru.homyakin.seeker.game.event.raid.models.RaidItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;

public record PersonageRaidResult(
    RaidParticipant participant,
    PersonageBattleStats stats,
    Money reward,
    Optional<RaidItem> generatedItem
) {
}
