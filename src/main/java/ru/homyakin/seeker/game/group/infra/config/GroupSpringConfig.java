package ru.homyakin.seeker.game.group.infra.config;

import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.group.entity.EventInterval;
import ru.homyakin.seeker.game.group.entity.EventIntervals;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.models.Money;

@ConfigurationProperties(prefix = "homyakin.seeker.group")
public class GroupSpringConfig implements GroupConfig {
    private static final Money REGISTRATION_PRICE = new Money(1000);
    private EventIntervals defaultEventIntervals;

    public EventIntervals defaultEventIntervals() {
        return defaultEventIntervals;
    }

    @Override
    public Money registrationPrice() {
        return REGISTRATION_PRICE;
    }

    /**
     * @param intervals строка в формате: "firstStartHour-firstEndHour;....."
     */
    public void setDefaultEventIntervals(String intervals) {
        final var eventIntervals = Arrays.stream(intervals.split(";"))
            .map(it -> it.split("-"))
            .map(it -> new EventInterval(Integer.parseInt(it[0]), Integer.parseInt(it[1]), true))
            .toList();
        defaultEventIntervals = new EventIntervals(eventIntervals);
    }
}
