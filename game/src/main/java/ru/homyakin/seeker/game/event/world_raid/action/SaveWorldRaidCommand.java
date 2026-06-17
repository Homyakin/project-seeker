package ru.homyakin.seeker.game.event.world_raid.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingWorldRaid;

@Component
public class SaveWorldRaidCommand {
    private final WorldRaidStorage storage;

    public SaveWorldRaidCommand(WorldRaidStorage storage) {
        this.storage = storage;
    }

    public void execute(int eventId, SavingWorldRaid raid) {
        storage.save(eventId, raid);
    }
}
