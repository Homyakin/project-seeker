package ru.homyakin.seeker.game.spin.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.spin.entity.EverydaySpinConfig;

@ConfigurationProperties(prefix = "homyakin.seeker.everyday-spin")
public record EverydaySpinSpringConfig(
    int minimumUsers
) implements EverydaySpinConfig {
}
