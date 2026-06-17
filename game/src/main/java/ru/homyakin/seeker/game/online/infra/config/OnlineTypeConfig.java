package ru.homyakin.seeker.game.online.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.online.OnlineTypeProvider;
import ru.homyakin.seeker.game.online.entity.OnlineType;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties("homyakin.seeker.online")
public class OnlineTypeConfig implements OnlineTypeProvider {
    private List<Duration> thresholds = List.of(
        Duration.ofDays(1),
        Duration.ofDays(3),
        Duration.ofDays(7),
        Duration.ofDays(14)
    );

    public OnlineTypeConfig() {
        validateThresholds(this.thresholds);
    }

    @Override
    public OnlineType convertDuration(Duration duration) {
        if (duration.isNegative()) {
            return OnlineType.ACTIVE;
        }
        final var types = OnlineType.values();
        for (int i = 0; i < thresholds.size(); i++) {
            if (duration.compareTo(thresholds.get(i)) <= 0) {
                return types[i];
            }
        }
        return types[types.length - 1];
    }

    public void setThresholds(String thresholds) {
        final var split = thresholds.split(",");
        this.thresholds = Arrays.stream(split)
            .map(Duration::parse)
            .toList();
        validateThresholds(this.thresholds);
    }

    private static void validateThresholds(List<Duration> thresholds) {
        if (thresholds.size() != OnlineType.values().length - 1) {
            throw new IllegalArgumentException("Online thresholds size must be " + (OnlineType.values().length - 1));
        }
        for (int i = 0; i < thresholds.size() - 1; ++i) {
            if (thresholds.get(i).compareTo(thresholds.get(i + 1)) >= 0) {
                throw new IllegalArgumentException("Online thresholds should be ordered asc");
            }
        }
    }
}
