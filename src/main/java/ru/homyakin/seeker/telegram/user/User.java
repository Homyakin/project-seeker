package ru.homyakin.seeker.telegram.user;

import ru.homyakin.seeker.locale.Language;
//TODO перенести юзера и чат в телегу
public record User(
    long id,
    boolean isActivePrivateMessages,
    Language language,
    long characterId
) {
    public boolean isSameLanguage(Language language) {
        return this.language == language;
    }
}
