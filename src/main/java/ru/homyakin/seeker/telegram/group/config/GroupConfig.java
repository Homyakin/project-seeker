package ru.homyakin.seeker.telegram.group.config;

import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.event.models.EventInterval;
import ru.homyakin.seeker.game.event.models.EventIntervals;

@ConfigurationProperties(prefix = "homyakin.seeker.group")
public class GroupConfig {
    private EventIntervals defaultEventIntervals;

    public EventIntervals defaultEventIntervals() {
        return defaultEventIntervals;
    }

    /**
     * @param intervals строка в формате: "firstStartHour-secondStartHour;....."
     */
    public void setDefaultEventIntervals(String intervals) {
        final var eventIntervals = Arrays.stream(intervals.split(";"))
            .map(it -> it.split("-"))
            .map(it -> new EventInterval(Integer.parseInt(it[0]), Integer.parseInt(it[1]), true))
            .toList();
        defaultEventIntervals = new EventIntervals(eventIntervals);
    }
}
