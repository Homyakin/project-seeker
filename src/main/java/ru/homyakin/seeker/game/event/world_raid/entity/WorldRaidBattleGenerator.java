package ru.homyakin.seeker.game.event.world_raid.entity;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.v3.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;

import java.util.List;

@Component
public class WorldRaidBattleGenerator {
    public List<BattlePersonage> generate(ActiveWorldRaid raid) {
        final var personage = new BattlePersonage(
            -1,
            new Characteristics(
                raid.info().health(),
                raid.info().attack(),
                raid.info().defense(),
                raid.info().strength(),
                raid.info().agility(),
                raid.info().wisdom()
            ),
            null
        );
        return List.of(personage);
    }
}
