package ru.homyakin.seeker.game.event.personal_quest.model;

import ru.homyakin.seeker.game.event.launched.EventPersonageParams;

public record PersonalQuestPersonageParams(
    int count
) implements EventPersonageParams {
}
