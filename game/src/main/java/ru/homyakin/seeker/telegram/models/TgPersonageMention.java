package ru.homyakin.seeker.telegram.models;

import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.telegram.user.models.UserId;

public class TgPersonageMention implements PersonageMention {
    private final String value;

    private TgPersonageMention(String value) {
        this.value = value;
    }

    public static TgPersonageMention of(Personage personage, UserId userId) {
        return new TgPersonageMention(
            "<a href=\"tg://user?id=" + userId.value() + "\">" + LocaleUtils.personageNameWithBadge(personage) + "</a>"
        );
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TgPersonageMention mention) {
            return mention.value.equals(this.value);
        }
        return false;
    }
}
