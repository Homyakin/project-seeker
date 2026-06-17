package ru.homyakin.seeker.game.event.world_raid.entity;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class ResearchGenerator {
    private final WorldRaidConfig config;

    public ResearchGenerator(WorldRaidConfig config) {
        this.config = config;
    }

    public ActiveWorldRaidState.Research generate() {
        return new ActiveWorldRaidState.Research(
            0,
            RandomUtils.getInPercentRange(config.averageRequiredContribution(), 10)
        );
    }
}
