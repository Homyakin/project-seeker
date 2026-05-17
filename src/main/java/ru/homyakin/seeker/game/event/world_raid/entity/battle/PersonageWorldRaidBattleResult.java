package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import ru.homyakin.seeker.game.battle.v3.two_team.PersonageBattleStats;
import ru.homyakin.seeker.game.item.models.LegacyItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

public record PersonageWorldRaidBattleResult(
    Personage personage,
    PersonageBattleStats stats,
    Money reward,
    Optional<LegacyItem> generatedItem
) {
    public PersonageId personageId() {
        return personage.id();
    }
}
