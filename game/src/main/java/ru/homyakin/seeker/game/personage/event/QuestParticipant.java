package ru.homyakin.seeker.game.personage.event;

import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestPersonageParams;
import ru.homyakin.seeker.game.personage.models.Personage;

public record QuestParticipant(
    Personage personage,
    PersonalQuestPersonageParams params
) {
}
