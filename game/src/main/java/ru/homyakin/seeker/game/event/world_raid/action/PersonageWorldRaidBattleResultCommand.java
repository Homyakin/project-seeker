package ru.homyakin.seeker.game.event.world_raid.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageBattleResult;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

@Component
public class PersonageWorldRaidBattleResultCommand {
    private final WorldRaidStorage storage;
    private final PersonageService personageService;

    public PersonageWorldRaidBattleResultCommand(
        WorldRaidStorage storage,
        PersonageService personageService
    ) {
        this.storage = storage;
        this.personageService = personageService;
    }

    public Optional<PersonageBattleResult> getForLastWorldRaid(PersonageId personageId) {
        return storage.getLaunchedEventIdForLastFinished()
            .flatMap(launchedEventId -> personageService.getBattleResult(personageId, launchedEventId));
    }
}
