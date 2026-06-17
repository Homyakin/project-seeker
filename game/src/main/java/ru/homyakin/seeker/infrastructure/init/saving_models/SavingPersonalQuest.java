package ru.homyakin.seeker.infrastructure.init.saving_models;

import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestLocale;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.util.Map;

public record SavingPersonalQuest(
    String code,
    boolean isEnabled,
    Map<Language, PersonalQuestLocale> locales
) implements Localized<PersonalQuestLocale> {
}
