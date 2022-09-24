package ru.homyakin.seeker.user;

import ru.homyakin.seeker.locale.Language;

public record User(
    //TODO переделать на player, добавить поле tg_user_id
    Long id,
    boolean isActivePrivateMessages,
    Language language
) {
    public boolean isSameLanguage(Language language) {
        return this.language == language;
    }
}
