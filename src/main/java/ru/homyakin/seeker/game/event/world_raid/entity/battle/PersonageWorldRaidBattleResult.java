package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.event.raid.models.RaidItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

public record PersonageWorldRaidBattleResult(
    Personage personage,
    BattlePersonageStats stats,
    Money reward,
    Optional<RaidItem> generatedItem
) {
    public PersonageId personageId() {
        return personage.id();
    }
}
