package ru.homyakin.seeker.game.event.launched;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RaidParams(
    int raidLevel,
    int raidPoints
) implements EventParams {
    public RaidParams withPoints(int points) {
        return new RaidParams(raidLevel, points);
    }
}
