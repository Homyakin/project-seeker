package ru.homyakin.seeker.game.worker.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.worker.entity.WorkerOfDayConfig;

@ConfigurationProperties(prefix = "homyakin.seeker.worker")
public record WorkerOfDaySpringConfig(
    int minimumMembers
) implements WorkerOfDayConfig {
}
