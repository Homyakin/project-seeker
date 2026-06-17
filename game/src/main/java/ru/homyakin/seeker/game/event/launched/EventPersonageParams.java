package ru.homyakin.seeker.game.event.launched;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestPersonageParams;
import ru.homyakin.seeker.game.event.raid.models.RaidPersonageParams;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RaidPersonageParams.class, name = "RaidPersonageParams"),
    @JsonSubTypes.Type(value = PersonalQuestPersonageParams.class, name = "PersonalQuestPersonageParams")
})
public interface EventPersonageParams {
}
