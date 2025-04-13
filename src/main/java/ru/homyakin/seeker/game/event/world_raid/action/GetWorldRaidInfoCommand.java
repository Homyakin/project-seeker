package ru.homyakin.seeker.game.event.world_raid.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidInfo;

@Component
public class GetWorldRaidInfoCommand {
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final WorldRaidConfig config;

    public GetWorldRaidInfoCommand(
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        WorldRaidConfig config
    ) {
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.config = config;
    }

    public WorldRaidInfo execute() {
        return new WorldRaidInfo(getOrLaunchWorldRaidCommand.execute(), config.requiredForDonate());
    }
}
