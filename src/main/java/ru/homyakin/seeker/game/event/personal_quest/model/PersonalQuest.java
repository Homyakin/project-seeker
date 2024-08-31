package ru.homyakin.seeker.game.event.personal_quest.model;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.util.Map;

public record PersonalQuest(
    int eventId,
    String code,
    Map<Language, PersonalQuestLocale> locales
) implements Localized<PersonalQuestLocale> {
}
