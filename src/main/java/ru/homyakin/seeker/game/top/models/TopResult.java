package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;

public interface TopResult {
    String toLocalizedString(Language language, PersonageId requestedPersonageId);
}
