package ru.homyakin.seeker.user;

import ru.homyakin.seeker.locale.Language;

public record User(
    Long id,
    boolean isActivePrivateMessages,
    Language language
) {
    public boolean isSameLanguage(Language language) {
        return this.language == language;
    }
}
