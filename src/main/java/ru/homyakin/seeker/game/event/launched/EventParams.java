package ru.homyakin.seeker.game.event.launched;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RaidParams.class, name = "RaidParams")
})
public interface EventParams {
}
